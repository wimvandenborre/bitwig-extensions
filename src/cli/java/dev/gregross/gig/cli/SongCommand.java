package dev.gregross.gig.cli;

import com.google.gson.*;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

@Command(
    name = "song",
    description = "Song dump and rebuild operations.",
    mixinStandardHelpOptions = true,
    subcommands = {
        SongCommand.DumpCommand.class,
        SongCommand.RebuildCommand.class
    }
)
class SongCommand {

    @ParentCommand
    GigCli parent;

    // --- Dump ---

    @Command(name = "dump", description = "Export the current Bitwig session to a song JSON file.",
             mixinStandardHelpOptions = true)
    static class DumpCommand implements Runnable {

        @ParentCommand
        private SongCommand songParent;

        @Option(names = {"--output", "-o"}, description = "Output file path (default: stdout)")
        private String outputPath;

        private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().create();
        private static final int SCENE_BANK_SIZE = 5;
        private static final int TRACK_BANK_SIZE = 8;
        private static final int CLIP_SETTLE_MS = 300;
        private static final int SCROLL_SETTLE_MS = 500;

        @Override
        public void run() {
            try {
                RpcClient client = songParent.parent.createClient();
                PrintStream log = System.err; // progress to stderr so stdout is clean JSON

                // Step 1: Get base snapshot
                log.println("[1/5] Reading session snapshot...");
                JsonObject snapshot = client.call("session/snapshot", null).getAsJsonObject();

                JsonObject transport = snapshot.getAsJsonObject("transport");
                JsonObject tracksSection = snapshot.getAsJsonObject("tracks");
                JsonObject scenesSection = snapshot.getAsJsonObject("scenes");
                JsonObject master = snapshot.getAsJsonObject("master");
                JsonObject device = snapshot.getAsJsonObject("device");
                JsonObject clip = snapshot.getAsJsonObject("clip");

                int trackCount = tracksSection.getAsJsonArray("tracks").size();
                int sceneItemCount = scenesSection.get("itemCount").getAsInt();

                // Step 2: Collect all scenes (may need to scroll)
                log.println("[2/5] Reading " + sceneItemCount + " scenes...");
                JsonArray allScenes = new JsonArray();
                JsonArray[][] clipSlots = new JsonArray[TRACK_BANK_SIZE][];
                for (int t = 0; t < TRACK_BANK_SIZE; t++) {
                    clipSlots[t] = new JsonArray[sceneItemCount];
                }

                int sceneBanks = (sceneItemCount + SCENE_BANK_SIZE - 1) / SCENE_BANK_SIZE;
                for (int bank = 0; bank < sceneBanks; bank++) {
                    int scrollPos = bank * SCENE_BANK_SIZE;
                    if (bank > 0) {
                        JsonObject scrollParams = new JsonObject();
                        scrollParams.addProperty("position", scrollPos);
                        client.call("sceneBank/scrollTo", scrollParams);
                        Thread.sleep(SCROLL_SETTLE_MS);
                        snapshot = client.call("session/snapshot", null).getAsJsonObject();
                        tracksSection = snapshot.getAsJsonObject("tracks");
                        scenesSection = snapshot.getAsJsonObject("scenes");
                    }

                    JsonArray bankScenes = scenesSection.getAsJsonArray("scenes");
                    JsonArray bankTracks = tracksSection.getAsJsonArray("tracks");

                    int scenesInBank = Math.min(SCENE_BANK_SIZE, sceneItemCount - scrollPos);
                    for (int s = 0; s < scenesInBank; s++) {
                        JsonObject scene = bankScenes.get(s).getAsJsonObject();
                        JsonObject sceneOut = new JsonObject();
                        sceneOut.addProperty("absoluteIndex", scrollPos + s);
                        sceneOut.addProperty("name", scene.get("name").getAsString());
                        sceneOut.add("color", scene.getAsJsonObject("color"));
                        allScenes.add(sceneOut);
                    }

                    // Capture clip hasContent per track in this bank window
                    for (int t = 0; t < Math.min(trackCount, TRACK_BANK_SIZE); t++) {
                        JsonObject track = bankTracks.get(t).getAsJsonObject();
                        JsonArray trackClips = track.getAsJsonArray("clips");
                        for (int s = 0; s < scenesInBank; s++) {
                            clipSlots[t][scrollPos + s] = new JsonArray();
                            JsonObject slotInfo = trackClips.get(s).getAsJsonObject();
                            if (slotInfo.get("hasContent").getAsBoolean()) {
                                // Store slot info — we'll read notes later
                                JsonObject marker = new JsonObject();
                                marker.addProperty("hasContent", true);
                                marker.addProperty("name", slotInfo.get("name").getAsString());
                                marker.add("color", slotInfo.getAsJsonObject("color"));
                                clipSlots[t][scrollPos + s].add(marker);
                            }
                        }
                    }
                }

                // Scroll back to 0
                if (sceneBanks > 1) {
                    JsonObject scrollParams = new JsonObject();
                    scrollParams.addProperty("position", 0);
                    client.call("sceneBank/scrollTo", scrollParams);
                    Thread.sleep(SCROLL_SETTLE_MS);
                }

                // Step 3: Read notes from each clip with content
                log.println("[3/5] Reading clip notes...");
                JsonArray allClips = new JsonArray();
                int clipCount = 0;
                int totalNotes = 0;

                for (int sceneAbs = 0; sceneAbs < sceneItemCount; sceneAbs++) {
                    // Ensure correct scene bank window
                    int neededScrollPos = (sceneAbs / SCENE_BANK_SIZE) * SCENE_BANK_SIZE;
                    int slotIndex = sceneAbs - neededScrollPos;

                    // Scroll if needed
                    JsonObject scrollParams = new JsonObject();
                    scrollParams.addProperty("position", neededScrollPos);
                    client.call("sceneBank/scrollTo", scrollParams);

                    for (int t = 0; t < Math.min(trackCount, TRACK_BANK_SIZE); t++) {
                        if (clipSlots[t][sceneAbs] == null || clipSlots[t][sceneAbs].isEmpty()) {
                            continue;
                        }

                        // Select this clip
                        JsonObject selectParams = new JsonObject();
                        selectParams.addProperty("trackIndex", t);
                        selectParams.addProperty("slotIndex", slotIndex);
                        selectParams.addProperty("force", true);
                        client.call("clip/select", selectParams);
                        Thread.sleep(CLIP_SETTLE_MS);

                        // Read clip metadata from snapshot
                        JsonObject clipSnap = client.call("session/snapshot", null)
                            .getAsJsonObject().getAsJsonObject("clip");

                        // Read notes
                        JsonElement notesResult = client.call("clip/getNotes", null);
                        JsonArray notes = notesResult.getAsJsonArray();

                        JsonObject clipInfo = clipSlots[t][sceneAbs].get(0).getAsJsonObject();

                        JsonObject clipOut = new JsonObject();
                        clipOut.addProperty("track", t);
                        clipOut.addProperty("scene", slotIndex);
                        clipOut.addProperty("sceneAbsolute", sceneAbs);
                        clipOut.addProperty("name", clipInfo.get("name").getAsString());
                        clipOut.addProperty("lengthBeats", clipSnap.get("loopLength").getAsDouble());
                        clipOut.addProperty("stepSize", clipSnap.get("stepSize").getAsDouble());
                        clipOut.add("color", clipInfo.getAsJsonObject("color"));
                        clipOut.add("notes", notes);
                        allClips.add(clipOut);

                        clipCount++;
                        totalNotes += notes.size();
                        log.println("  [" + clipCount + "] Track " + t + " / Scene " + sceneAbs
                            + " (" + notes.size() + " notes)");
                    }
                }

                // Step 4: Read track info (instruments, drums)
                log.println("[4/5] Reading track instruments...");
                JsonArray instruments = new JsonArray();
                JsonArray drumPadMap = new JsonArray();

                // Select each track and read device info
                for (int t = 0; t < trackCount; t++) {
                    JsonObject selectParams = new JsonObject();
                    selectParams.addProperty("index", t);
                    client.call("track/select", selectParams);
                    Thread.sleep(CLIP_SETTLE_MS);

                    // Read device info
                    JsonObject devSnap = client.call("session/snapshot", null)
                        .getAsJsonObject().getAsJsonObject("device");
                    String deviceName = devSnap.get("name").getAsString();
                    String presetName = devSnap.get("presetName").getAsString();

                    if (deviceName != null && !deviceName.isEmpty()) {
                        JsonObject inst = new JsonObject();
                        inst.addProperty("track", t);
                        inst.addProperty("device", deviceName);
                        inst.addProperty("preset", presetName != null ? presetName : "");
                        instruments.add(inst);
                    }

                    // Check for drum pads
                    if (devSnap.has("hasDrumPads") && devSnap.get("hasDrumPads").getAsBoolean()) {
                        try {
                            JsonElement padsResult = client.call("device/getDrumPads", null);
                            if (padsResult.isJsonArray()) {
                                for (JsonElement pad : padsResult.getAsJsonArray()) {
                                    drumPadMap.add(pad);
                                }
                            }
                        } catch (IOException e) {
                            log.println("  Warning: Could not read drum pads for track " + t);
                        }
                    }
                }

                // Step 5: Read cue markers
                log.println("[5/5] Reading cue markers...");
                JsonObject arrangementSnap = client.call("session/snapshot", null)
                    .getAsJsonObject().getAsJsonObject("arrangement");
                JsonArray cueMarkers = new JsonArray();
                if (arrangementSnap != null && arrangementSnap.has("cueMarkers")) {
                    cueMarkers = arrangementSnap.getAsJsonArray("cueMarkers");
                }

                // Assemble song JSON
                JsonObject song = new JsonObject();

                // Meta
                JsonObject meta = new JsonObject();
                meta.addProperty("formatVersion", "1");
                meta.addProperty("name", "");
                meta.addProperty("key", "");
                meta.addProperty("created", LocalDate.now().toString());
                meta.addProperty("version", "gig-maestro-v0.21.1");
                song.add("meta", meta);

                // Transport
                JsonObject transportOut = new JsonObject();
                transportOut.addProperty("tempo", transport.get("tempo").getAsDouble());
                transportOut.addProperty("timeSignature", transport.get("timeSignature").getAsString());
                song.add("transport", transportOut);

                // Tracks
                JsonArray tracksOut = new JsonArray();
                JsonArray bankTracks = tracksSection.getAsJsonArray("tracks");
                for (int t = 0; t < Math.min(trackCount, TRACK_BANK_SIZE); t++) {
                    // Re-read from original snapshot for accurate track data
                    // (we may have scrolled scenes, but tracks don't change)
                    JsonObject origTrack;
                    if (t < bankTracks.size()) {
                        origTrack = bankTracks.get(t).getAsJsonObject();
                    } else {
                        continue;
                    }
                    JsonObject trackOut = new JsonObject();
                    trackOut.addProperty("index", t);
                    trackOut.addProperty("name", origTrack.get("name").getAsString());
                    trackOut.addProperty("volume", origTrack.get("volume").getAsDouble());
                    trackOut.addProperty("pan", origTrack.get("pan").getAsDouble());
                    trackOut.addProperty("mute", origTrack.get("mute").getAsBoolean());
                    trackOut.addProperty("solo", origTrack.get("solo").getAsBoolean());
                    trackOut.add("color", origTrack.getAsJsonObject("color"));
                    tracksOut.add(trackOut);
                }
                song.add("tracks", tracksOut);

                // Master
                JsonObject masterOut = new JsonObject();
                masterOut.addProperty("volume", master.get("volume").getAsDouble());
                masterOut.addProperty("pan", master.get("pan").getAsDouble());
                song.add("master", masterOut);

                // Scenes
                song.add("scenes", allScenes);

                // Clips
                song.add("clips", allClips);

                // Instruments + Drum pad map
                song.add("instruments", instruments);
                song.add("drumPadMap", drumPadMap);

                // Cue markers
                song.add("cueMarkers", cueMarkers);

                // Stats
                JsonObject stats = new JsonObject();
                stats.addProperty("totalClips", clipCount);
                stats.addProperty("totalNotes", totalNotes);
                stats.addProperty("totalScenes", sceneItemCount);
                stats.addProperty("totalCueMarkers", cueMarkers.size());
                song.add("stats", stats);

                // Output
                String json = PRETTY.toJson(song);
                if (outputPath != null) {
                    Files.writeString(Path.of(outputPath), json);
                    log.println("Song exported to: " + outputPath);
                } else {
                    System.out.println(json);
                }

                log.println("Done! " + clipCount + " clips, " + totalNotes + " notes, "
                    + sceneItemCount + " scenes.");

            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    // --- Rebuild ---

    @Command(name = "rebuild", description = "Rebuild a Bitwig session from a song JSON file.",
             mixinStandardHelpOptions = true)
    static class RebuildCommand implements Runnable {

        @ParentCommand
        private SongCommand songParent;

        @Parameters(index = "0", description = "Path to the song JSON file")
        private String filePath;

        private static final int SCENE_SETTLE_MS = 500;
        private static final int CLIP_WRITE_MS = 200;
        private static final int CLIP_COLOR_MS = 50;
        private static final int CUE_MARKER_MS = 200;
        private static final int SCROLL_SETTLE_MS = 300;
        private static final int SCENE_BANK_SIZE = 5;

        @Override
        public void run() {
            System.err.println("Rebuild not yet implemented. Coming in v0.21.2.");
            System.exit(1);
        }
    }
}

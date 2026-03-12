package com.gregross.bitwig.launchpadmk2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.gregross.bitwig.launchpadmk2.LaunchpadMk2Colors.*;
import static org.junit.jupiter.api.Assertions.*;

class LaunchpadMk2ColorsTest {

    // --- gridNote ---

    @Test
    void gridNote_origin() {
        assertEquals(11, gridNote(0, 0));
    }

    @Test
    void gridNote_maxCorner() {
        assertEquals(88, gridNote(7, 7));
    }

    @Test
    void gridNote_midRange() {
        assertEquals(45, gridNote(3, 4));
    }

    // --- sceneLaunchNote ---

    @Test
    void sceneLaunchNote_firstRow() {
        assertEquals(19, sceneLaunchNote(0));
    }

    @Test
    void sceneLaunchNote_lastRow() {
        assertEquals(89, sceneLaunchNote(7));
    }

    // --- packRgb: hue zone coverage ---

    @ParameterizedTest(name = "packRgb hue zone: {6}")
    @CsvSource({
        // r,    g,    b,    label
        "1.0,  0.0,  0.0,  red",
        "1.0,  0.4,  0.0,  orange",
        "1.0,  0.9,  0.0,  yellow",
        "0.5,  1.0,  0.0,  lime",
        "0.0,  1.0,  0.0,  green",
        "0.0,  1.0,  0.5,  mint",
        "0.0,  1.0,  1.0,  cyan",
        "0.0,  0.5,  1.0,  lightBlue",
        "0.0,  0.0,  1.0,  blue",
        "0.5,  0.0,  1.0,  purple",
        "1.0,  0.0,  1.0,  magenta",
        "1.0,  0.0,  0.5,  pink"
    })
    void packRgb_hueZones(float r, float g, float b, String label) {
        int packed = packRgb(r, g, b);
        // Packed value must be non-negative and fit in 18 bits (3 channels × 6 bits)
        assertTrue(packed >= 0, label + ": packed must be >= 0");
        assertTrue(packed <= 0x3FFFF, label + ": packed must fit in 18 bits");

        // At least one channel should be non-zero for saturated colors
        int ri = (packed >> 12) & 0x3F;
        int gi = (packed >> 6) & 0x3F;
        int bi = packed & 0x3F;
        assertTrue(ri > 0 || gi > 0 || bi > 0, label + ": at least one channel non-zero");
    }

    @Test
    void packRgb_pureRed_killsGreenAndDimsBlue() {
        int packed = packRgb(1.0f, 0.0f, 0.0f);
        int ri = (packed >> 12) & 0x3F;
        int gi = (packed >> 6) & 0x3F;
        int bi = packed & 0x3F;
        assertEquals(63, ri, "red channel maxed");
        assertEquals(0, gi, "green channel killed");
        assertEquals(0, bi, "blue channel zero (input was 0)");
    }

    @Test
    void packRgb_pureGreen_killsRedAndReducesBlue() {
        int packed = packRgb(0.0f, 1.0f, 0.0f);
        int ri = (packed >> 12) & 0x3F;
        int gi = (packed >> 6) & 0x3F;
        int bi = packed & 0x3F;
        assertEquals(0, ri, "red channel killed");
        assertEquals(63, gi, "green channel maxed");
        assertEquals(0, bi, "blue channel zero (input was 0)");
    }

    @Test
    void packRgb_pureBlue_killsRedAndReducesGreen() {
        int packed = packRgb(0.0f, 0.0f, 1.0f);
        int ri = (packed >> 12) & 0x3F;
        int gi = (packed >> 6) & 0x3F;
        int bi = packed & 0x3F;
        assertEquals(0, ri, "red killed");
        assertEquals(0, gi, "green killed");
        assertEquals(63, bi, "blue channel maxed");
    }

    @Test
    void packRgb_lowSaturation_noCorrection() {
        // Near-white: delta < 0.05, no hue correction applied
        int packed = packRgb(0.9f, 0.9f, 0.9f);
        int ri = (packed >> 12) & 0x3F;
        int gi = (packed >> 6) & 0x3F;
        int bi = packed & 0x3F;
        // All channels should be close (no correction applied)
        assertEquals(ri, gi, "R ≈ G for near-white");
        assertEquals(gi, bi, "G ≈ B for near-white");
    }

    @Test
    void packRgb_nearBlack_returnsZero() {
        int packed = packRgb(0.01f, 0.01f, 0.01f);
        assertEquals(0, packed, "near-black packs to zero");
    }

    // --- dimPackedRgb ---

    @Test
    void dimPackedRgb_reducesBrightness() {
        // Pack full red: r=63, g=0, b=0
        int packed = (63 << 12);
        int dimmed = dimPackedRgb(packed);
        int ri = (dimmed >> 12) & 0x3F;
        // 63 * 4 / 10 = 25 (integer division)
        assertEquals(25, ri, "red dimmed to ~40%");
    }

    @Test
    void dimPackedRgb_allChannels() {
        int packed = (40 << 12) | (30 << 6) | 20;
        int dimmed = dimPackedRgb(packed);
        assertEquals((40 * 4 / 10), (dimmed >> 12) & 0x3F);
        assertEquals((30 * 4 / 10), (dimmed >> 6) & 0x3F);
        assertEquals((20 * 4 / 10), dimmed & 0x3F);
    }

    // --- setLedRgb ---

    @Test
    void setLedRgb_sysExFormat() {
        int packed = (10 << 12) | (20 << 6) | 30;
        byte[] sysex = setLedRgb(60, packed);
        assertEquals(12, sysex.length);
        assertEquals((byte) 0xF0, sysex[0], "SysEx start");
        assertEquals((byte) 0x00, sysex[1]);
        assertEquals((byte) 0x20, sysex[2]);
        assertEquals((byte) 0x29, sysex[3], "Novation manufacturer ID");
        assertEquals((byte) 0x02, sysex[4]);
        assertEquals((byte) 0x18, sysex[5], "MK2 product ID");
        assertEquals((byte) 0x0B, sysex[6], "RGB LED command");
        assertEquals((byte) 60, sysex[7], "note number");
        assertEquals((byte) 10, sysex[8], "red channel");
        assertEquals((byte) 20, sysex[9], "green channel");
        assertEquals((byte) 30, sysex[10], "blue channel");
        assertEquals((byte) 0xF7, sysex[11], "SysEx end");
    }

    // --- findClosestVelocity ---

    @ParameterizedTest(name = "findClosestVelocity: {3} → velocity {4}")
    @CsvSource({
        "1.0,  0.0,  0.0,  red,       5",
        "1.0,  0.4,  0.0,  orange,    9",
        "1.0,  0.9,  0.0,  yellow,   13",
        "0.5,  1.0,  0.0,  lime,     17",
        "0.0,  1.0,  0.0,  green,    21",
        "0.0,  1.0,  0.5,  mint,     29",
        "0.0,  1.0,  1.0,  cyan,     37",
        "0.0,  0.5,  1.0,  lightBlue,41",
        "0.0,  0.0,  1.0,  blue,     45",
        "0.5,  0.0,  1.0,  purple,   49",
        "1.0,  0.0,  1.0,  magenta,  53",
        "1.0,  0.0,  0.5,  pink,     57"
    })
    void findClosestVelocity_hueZones(float r, float g, float b, String label, int expectedVelocity) {
        assertEquals(expectedVelocity, findClosestVelocity(r, g, b), label);
    }

    @Test
    void findClosestVelocity_lowSaturation_returnsWhite() {
        assertEquals(3, findClosestVelocity(0.8f, 0.8f, 0.8f));
    }

    // --- flashLed ---

    @Test
    void flashLed_sysExFormat() {
        byte[] sysex = flashLed(44, 5);
        assertEquals(10, sysex.length);
        assertEquals((byte) 0xF0, sysex[0]);
        assertEquals((byte) 0x23, sysex[6], "flash command");
        assertEquals((byte) 44, sysex[7], "note");
        assertEquals((byte) 5, sysex[8], "color");
        assertEquals((byte) 0xF7, sysex[9]);
    }

    // --- pulseLed ---

    @Test
    void pulseLed_sysExFormat() {
        byte[] sysex = pulseLed(55, 61);
        assertEquals(10, sysex.length);
        assertEquals((byte) 0x28, sysex[6], "pulse command");
        assertEquals((byte) 55, sysex[7], "note");
        assertEquals((byte) 61, sysex[8], "color");
    }

    // --- resetLeds ---

    @Test
    void resetLeds_sysExFormat() {
        byte[] sysex = resetLeds();
        assertEquals(9, sysex.length);
        assertEquals((byte) 0xF0, sysex[0]);
        assertEquals((byte) 0x0E, sysex[6], "reset command");
        assertEquals((byte) 0x00, sysex[7]);
        assertEquals((byte) 0xF7, sysex[8]);
    }

    // --- setSessionLayout ---

    @Test
    void setSessionLayout_sysExFormat() {
        byte[] sysex = setSessionLayout();
        assertEquals(9, sysex.length);
        assertEquals((byte) 0xF0, sysex[0]);
        assertEquals((byte) 0x22, sysex[6], "layout command");
        assertEquals((byte) 0x00, sysex[7]);
        assertEquals((byte) 0xF7, sysex[8]);
    }
}

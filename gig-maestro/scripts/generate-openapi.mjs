#!/usr/bin/env node

/**
 * Generates an OpenAPI 3.1 spec from claude-tools.json.
 *
 * Each Claude tool (e.g. "transport_play") maps to a POST path
 * (e.g. "/transport/play") wrapping the tool's input_schema
 * inside a JSON-RPC 2.0 request envelope.
 *
 * Usage: node scripts/generate-openapi.mjs
 * Output: docs/openapi.json
 */

import { readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const root = join(__dirname, "..");

const tools = JSON.parse(readFileSync(join(root, "tools/claude-tools.json"), "utf-8"));

// Derive tag (domain) from tool name: "transport_play" → "transport"
function getTag(name) {
  const prefix = name.split("_")[0];
  const tagMap = {
    session: "Session",
    api: "Session",
    transport: "Transport",
    track: "Track",
    trackBank: "Track Bank",
    master: "Master",
    cursor: "Cursor",
    clip: "Clip",
    note: "Note",
    noteInput: "Note Input",
    scene: "Scene",
    sceneBank: "Scene Bank",
    device: "Device",
    masterDevice: "Master Device",
    browser: "Browser",
    arranger: "Arranger",
    arrangement: "Arranger",
    detailEditor: "Detail Editor",
    mixer: "Mixer",
    project: "Project",
    groove: "Groove",
    send: "Send",
    arpeggiator: "Arpeggiator",
    noteLatch: "Note Latch",
    cueMarker: "Cue Marker",
    cueMarkerBank: "Cue Marker Bank",
    macro: "Macro",
    state: "State",
    action: "Action",
    app: "Application",
  };
  // Handle multi-word prefixes like "masterDevice_selectNext"
  for (const key of Object.keys(tagMap).sort((a, b) => b.length - a.length)) {
    if (name.startsWith(key + "_")) return tagMap[key];
  }
  return tagMap[prefix] || prefix;
}

// Convert tool name to RPC method path: "transport_play" → "/transport/play"
function toPath(name) {
  // Handle multi-segment domains: masterDevice_selectNext → /masterDevice/selectNext
  // Find the longest matching prefix
  const domains = [
    "session", "api", "transport", "track", "trackBank", "master",
    "cursor", "clip", "note", "noteInput", "scene", "sceneBank",
    "device", "masterDevice", "browser", "arranger", "arrangement",
    "detailEditor", "mixer", "project", "groove", "send",
    "arpeggiator", "noteLatch", "cueMarker", "cueMarkerBank",
    "macro", "state", "action", "app",
  ].sort((a, b) => b.length - a.length);

  for (const domain of domains) {
    if (name.startsWith(domain + "_")) {
      const method = name.slice(domain.length + 1);
      // Convert remaining underscores to camelCase continuation (they're already camelCase)
      return `/${domain}/${method}`;
    }
  }
  // Fallback: first underscore splits domain/method
  const idx = name.indexOf("_");
  if (idx === -1) return `/${name}`;
  return `/${name.slice(0, idx)}/${name.slice(idx + 1)}`;
}

// Convert tool name to RPC method name: "transport_play" → "transport/play"
function toRpcMethod(name) {
  return toPath(name).slice(1); // strip leading /
}

// Build paths
const paths = {};
const tagSet = new Set();

for (const tool of tools) {
  const path = toPath(tool.name);
  const rpcMethod = toRpcMethod(tool.name);
  const tag = getTag(tool.name);
  tagSet.add(tag);

  const hasParams =
    tool.input_schema.properties &&
    Object.keys(tool.input_schema.properties).length > 0;

  const requestBody = {
    required: true,
    content: {
      "application/json": {
        schema: {
          type: "object",
          required: ["jsonrpc", "method", "id"],
          properties: {
            jsonrpc: { type: "string", enum: ["2.0"], description: "JSON-RPC version" },
            method: { type: "string", enum: [rpcMethod], description: "RPC method name" },
            id: { type: "integer", description: "Request identifier", example: 1 },
            params: hasParams
              ? { ...tool.input_schema, description: "Method parameters" }
              : { type: "object", properties: {}, description: "No parameters required" },
          },
        },
        example: {
          jsonrpc: "2.0",
          method: rpcMethod,
          params: hasParams ? buildExample(tool.input_schema) : {},
          id: 1,
        },
      },
    },
  };

  paths[path] = {
    post: {
      tags: [tag],
      summary: firstSentence(tool.description),
      description: tool.description,
      operationId: tool.name,
      requestBody,
      responses: {
        200: {
          description: "JSON-RPC 2.0 response",
          content: {
            "application/json": {
              schema: { $ref: "#/components/schemas/JsonRpcResponse" },
            },
          },
        },
      },
    },
  };
}

// Build example params from schema
function buildExample(schema) {
  const example = {};
  if (!schema.properties) return example;
  for (const [key, prop] of Object.entries(schema.properties)) {
    if (prop.type === "string") example[key] = prop.enum?.[0] || "string";
    else if (prop.type === "number" || prop.type === "integer") example[key] = 0;
    else if (prop.type === "boolean") example[key] = true;
    else if (prop.type === "array") example[key] = [];
    else if (prop.type === "object") example[key] = {};
  }
  return example;
}

function firstSentence(text) {
  const match = text.match(/^(.+?\.)\s/);
  return match ? match[1] : text.slice(0, 120);
}

// Sort tags in logical order
const tagOrder = [
  "Session", "Transport", "Track", "Track Bank", "Master", "Cursor",
  "Clip", "Note", "Note Input", "Scene", "Scene Bank",
  "Device", "Master Device", "Browser",
  "Arranger", "Detail Editor", "Mixer", "Project", "Groove", "Send",
  "Arpeggiator", "Note Latch", "Cue Marker", "Cue Marker Bank",
  "Macro", "State", "Action", "Application",
];

const tags = tagOrder
  .filter((t) => tagSet.has(t))
  .map((name) => ({ name }));

const spec = {
  openapi: "3.1.0",
  info: {
    title: "Gig Maestro — Bitwig Studio RPC API",
    version: "0.42.0",
    description:
      "Control Bitwig Studio programmatically via JSON-RPC 2.0. " +
      "All methods are invoked as POST requests to a single endpoint (`/rpc`). " +
      "This spec models each RPC method as its own path for interactive documentation. " +
      "In practice, all requests go to `POST /rpc` with the method name in the JSON body.\n\n" +
      "**HTTP Endpoint:** `POST http://localhost:8787/rpc`\n\n" +
      "**WebSocket Endpoint:** `ws://localhost:8788/` (real-time state streaming)\n\n" +
      "**Protocol:** JSON-RPC 2.0",
  },
  servers: [
    {
      url: "http://localhost:8787",
      description: "Local Bitwig Studio (Gig Maestro extension)",
    },
  ],
  tags,
  paths,
  components: {
    schemas: {
      JsonRpcResponse: {
        type: "object",
        properties: {
          jsonrpc: { type: "string", enum: ["2.0"] },
          result: { type: "object", description: "Method-specific result data" },
          error: {
            type: "object",
            properties: {
              code: { type: "integer", description: "Error code" },
              message: { type: "string", description: "Error message" },
            },
          },
          id: { type: "integer", description: "Echoed request identifier" },
        },
      },
    },
  },
};

const output = join(root, "docs/openapi.json");
writeFileSync(output, JSON.stringify(spec, null, 2) + "\n");
console.log(`Generated ${output} — ${tools.length} methods, ${tags.length} tags`);

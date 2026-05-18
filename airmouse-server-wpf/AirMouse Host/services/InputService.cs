using AirMouse_Host.input;
using AirMouse_Host.models;
using System;
using System.Collections.Generic;
using System.Text;
using System.Text.Json;

namespace AirMouse_Host.services
{
    internal class InputService
    {
        private readonly MouseHandler mouse = new();
        private readonly KeyMapper keyboard = new();

        public void Process(InputPacket packet)
        {
            switch (packet.Action)
            {
                case "mouse_move":
                    HandleMouseMove(packet.Payload);
                    break;

                case "mouse_button":
                    HandleMouseButton(packet.Payload);
                    break;

                case "mouse_scroll":
                    HandleScroll(packet.Payload);
                    break;

                case "key_press":
                    HandleKey(packet.Payload);
                    break;

                case "key_combo":
                    HandleCombo(packet.Payload);
                    break;
            }
        }

        private void HandleMouseMove(string payload)
        {
            try
            {
                using var doc = JsonDocument.Parse(payload);
                var root = doc.RootElement;

                int dx = GetIntFromJson(root, "DX");
                int dy = GetIntFromJson(root, "DY");

                mouse.Move(dx, dy);
            }
            catch
            {
                // Fallback to previous behavior on parse error
                var data = JsonSerializer.Deserialize<MousePayload>(payload) ?? new MousePayload();
                mouse.Move(data.DX.GetValueOrDefault(), data.DY.GetValueOrDefault());
            }
        }

        private void HandleMouseButton(string payload)
        {
            try
            {
                using var doc = JsonDocument.Parse(payload);
                var root = doc.RootElement;

                string button = GetStringFromJson(root, "Button");
                string state = GetStringFromJson(root, "State");

                mouse.Click(button, state);
            }
            catch
            {
                var data = JsonSerializer.Deserialize<MousePayload>(payload) ?? new MousePayload();
                mouse.Click(data.Button ?? string.Empty, data.State ?? string.Empty);
            }
        }

        private void HandleScroll(string payload)
        {
            try
            {
                using var doc = JsonDocument.Parse(payload);
                var root = doc.RootElement;

                int scroll = GetIntFromJson(root, "Scroll");

                mouse.Scroll(scroll);
            }
            catch
            {
                var data = JsonSerializer.Deserialize<MousePayload>(payload) ?? new MousePayload();
                mouse.Scroll(data.Scroll.GetValueOrDefault());
            }
        }

        private static bool TryGetPropertyIgnoreCase(JsonElement root, string name, out JsonElement prop)
        {
            foreach (var p in root.EnumerateObject())
            {
                if (string.Equals(p.Name, name, StringComparison.OrdinalIgnoreCase))
                {
                    prop = p.Value;
                    return true;
                }
            }

            prop = default;
            return false;
        }

        private static int GetIntFromJson(JsonElement root, string name)
        {
            if (TryGetPropertyIgnoreCase(root, name, out var prop))
            {
                try
                {
                    if (prop.ValueKind == JsonValueKind.Number)
                    {
                        if (prop.TryGetInt32(out int i))
                            return i;

                        if (prop.TryGetDouble(out double d))
                            return (int)Math.Round(d);
                    }

                    if (prop.ValueKind == JsonValueKind.String)
                    {
                        var s = prop.GetString();
                        if (int.TryParse(s, out int si))
                            return si;

                        if (double.TryParse(s, System.Globalization.NumberStyles.Float, System.Globalization.CultureInfo.InvariantCulture, out double sd))
                            return (int)Math.Round(sd);
                    }
                }
                catch
                {
                    // fall through to default
                }
            }

            return 0;
        }

        private static string GetStringFromJson(JsonElement root, string name)
        {
            if (TryGetPropertyIgnoreCase(root, name, out var prop))
            {
                try
                {
                    if (prop.ValueKind == JsonValueKind.String)
                        return prop.GetString() ?? string.Empty;

                    // Try numeric -> string
                    if (prop.ValueKind == JsonValueKind.Number)
                        return prop.GetRawText();
                }
                catch
                {
                    // ignore
                }
            }

            return string.Empty;
        }

        private void HandleKey(string payload)
        {
            var data = JsonSerializer.Deserialize<KeyPayload>(payload);
            keyboard.Press(data.Key);
        }

        private void HandleCombo(string payload)
        {
            var data = JsonSerializer.Deserialize<KeyPayload>(payload);
            keyboard.Combo(data.Keys);
        }
    }
}

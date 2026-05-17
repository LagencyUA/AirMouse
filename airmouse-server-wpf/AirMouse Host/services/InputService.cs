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
            var data = JsonSerializer.Deserialize<MousePayload>(payload);
            mouse.Move(data.DX, data.DY);
        }

        private void HandleMouseButton(string payload)
        {
            var data = JsonSerializer.Deserialize<MousePayload>(payload);
            mouse.Click(data.Button, data.State);
        }

        private void HandleScroll(string payload)
        {
            var data = JsonSerializer.Deserialize<MousePayload>(payload);
            mouse.Scroll(data.Scroll);
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

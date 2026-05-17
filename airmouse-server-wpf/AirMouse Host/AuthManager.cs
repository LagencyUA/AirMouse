using AirMouse_Host.models;
using System;
using System.Linq;

namespace AirMouse_Host
{
    public class AuthManager
    {
        private readonly int _codeLength;
        private const string Characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        private static readonly Random _rnd = new Random();

        public string CurrentPin { get; private set; }
        public AppSettings Settings { get; private set; }

        public AuthManager(AppSettings settings, int codeLength = 6)
        {
            _codeLength = codeLength;
            Settings = settings;
            RefreshPin();
        }
        public void RefreshPin()
        {
            if (Settings.UseStaticPin)
            {
                CurrentPin = Settings.StaticPin;
            }
            else
            {
                GenerateDynamicPin();
            }
        }
        public void GenerateDynamicPin()
        {
            char[] result = new char[_codeLength];
            for (int i = 0; i < _codeLength; i++)
            {
                result[i] = Characters[_rnd.Next(Characters.Length)];
            }

            CurrentPin = new string(result);
        }

        public void ToggleStaticPin(bool useStatic, string customPin = null)
        {
            Settings.UseStaticPin = useStatic;
            if (useStatic && !string.IsNullOrEmpty(customPin))
            {
                Settings.StaticPin = customPin;
            }

            SettingsManager.Save(Settings);
            RefreshPin();
        }

        public bool Validate(string pin)
        {
            return string.Equals(pin, CurrentPin, StringComparison.Ordinal);
        }
    }
}
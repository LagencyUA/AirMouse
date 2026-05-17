using AirMouse_Host.models;
using System;
using System.Collections.Generic;
using System.Text;
using System.Text.Json;

namespace AirMouse_Host
{
    public static class SettingsManager
    {
        private static readonly string FilePath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "AirMouseHost", "settings.json"
    );

        public static AppSettings Load()
        {
            try
            {
                if (!File.Exists(FilePath)) return new AppSettings();
                string json = File.ReadAllText(FilePath);
                return JsonSerializer.Deserialize<AppSettings>(json) ?? new AppSettings();
            }
            catch { return new AppSettings(); } // create default settings if not exists or on error
        }

        public static void Save(AppSettings settings)
        {
            try
            {
                string dir = Path.GetDirectoryName(FilePath);
                if (!Directory.Exists(dir))
                    Directory.CreateDirectory(dir);

                string json = JsonSerializer.Serialize(settings, new JsonSerializerOptions { WriteIndented = true });

                File.WriteAllText(FilePath, json);
            }
            catch { }
        }
    }
}

# Drowz release rules. Kept minimal for v1; add keeps here as native/service
# reflection surfaces grow (e.g. AlarmManager receivers referenced only in the manifest
# are already kept by AGP's manifest-aware shrinker).
-dontobfuscate

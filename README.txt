BINA LINUX KE JAR BANANE KA TAREEQA (sirf Chrome browser):
1. github.com par free account banao.
2. New repository banao (naam: elytramacesmash, Public).
3. "uploading an existing file" par click karo, is zip ko Chromebook mein extract karo
   (Files app: zip par double-click > Extract all), phir elytramacesmash folder ke ANDAR ki saari cheezein
   drag-drop karo (src, build.gradle, settings.gradle, gradle.properties, .github).
   Agar .github folder upload na ho: Add file > Create new file, naam mein
   .github/workflows/build.yml likho aur build.yml ka text paste karo.
4. Commit karo. Upar "Actions" tab kholo, "Build mod" chalne do (3-5 min), green tick ka wait.
5. Us run par click karo > neeche Artifacts > elytramacesmash-jar download karo > zip extract karo > .jar milega.
6. Jar ko Minecraft ke mods folder mein rakho (Fabric Loader + Fabric API + Mod Menu bhi chahiye).

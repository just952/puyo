 = Get-Content android/build.gradle -Raw -Encoding UTF8  
 =  -replace 'implementation 'com.badlogicgames.gdx:gdx-backend-android:'', 'implementation " "com.badlogicgames.gdx:gdx-backend-android:'  
 =  -replace 'implementation 'com.badlogicgames.gdx:gdx-platform:-armeabi-v7a'', 'implementation com.badlogicgames.gdx:gdx-platform:-armeabi-v7a'  
 =  -replace 'implementation 'com.badlogicgames.gdx:gdx-platform:-arm64-v8a'', 'implementation com.badlogicgames.gdx:gdx-platform:-arm64-v8a'  
 =  -replace 'implementation 'com.badlogicgames.gdx:gdx-freetype-platform:-arm64-v8a'', 'implementation com.badlogicgames.gdx:gdx-freetype:'  

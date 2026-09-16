# Debug/sideload APK ships unminified. Keep rules ready for a later release build.
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.**
-dontwarn org.etd.dev.**
-dontwarn org.w3c.dom.**

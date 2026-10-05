# Default ProGuard rules (minification is disabled for the MVP).
# Keep kotlinx.serialization generated serializers if minification is ever enabled.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

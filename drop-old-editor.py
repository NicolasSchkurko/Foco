import io, os

# The old editor is no longer used in production: the app settings now shows the quick adjust UI.
os.remove('app/src/main/java/com/foco/app/ui/detail/AppLimitEditor.kt')
print('removed AppLimitEditor.kt')

# Its test goes with it; a new test covers the new UI.
os.remove('app/src/androidTest/java/com/foco/app/ui/AppLimitsSettingsTest.kt')
print('removed AppLimitsSettingsTest.kt')

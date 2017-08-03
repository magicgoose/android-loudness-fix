# android-loudness-fix
Automatically patches build.prop to prevent Android from resetting loudness to a small value while playing music.  
Obviously, this requires root.

Basically it automates the action described here https://www.reddit.com/r/Android/comments/3yjyrk/you_can_bypass_safe_volume_warning_on_60_using/ including the remount thing to make the file temporarily writable, and checks if it's already patched before making the change.

_I hold no responsibility for any damages whatsoever, please read the code before using on production devices._

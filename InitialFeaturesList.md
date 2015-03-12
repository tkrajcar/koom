# Introduction #

  * Normal client features (copy/paste, saved world list, autologin)
  * All existing THUD features (LOS computation, weapon arcs and ranges superimposed on tacmap, zoom in/out tacmap)
  * Embedded, well-documented scripting language
  * Right click on map for options - lock target hex,  lock unit, scan, etc (http://sourceforge.net/tracker/?func=detail&aid=1435575&group_id=58787&atid=488873)
  * double click on point on map to change heading to precisely that point (not just hex, but point within hex)
  * Smart tacmap label drawing so mech labels are not superimposed
  * Automatically append called cons to the contacts list/tacmap if they're not in your LOS, with expiration after 60 seconds (update if re-called)
  * Full user-configurable keybind options
  * Option for a 'wake trigger' - when client sees a user-defined block of text (typically something like 'wake CE') it plays a sound, raises a dialog, otherwise gets your attention
  * Multiple tac windows (for big picture/closeup)
  * Armor diagram window
  * Weapon specs window
  * Crit status window
  * Searchable mechref database
  * Target status window that is persistent and is updated whenever you do a 'scan' (this will be a bit tricky but i have confidences)
  * Display in status window of current target - bearing, range, arc facing, facing arc, and indication on weapon status of which range bth modifier the target is at
  * When target locked, dim weapons that are out of range and/or out of arc
  * All settings saved in profiles that can be loaded/saved, for people that go between various monitor configurations
  * Window opacity
  * Proxy support
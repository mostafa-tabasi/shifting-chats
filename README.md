# Shifting Chats

A simple chat UI in Jetpack Compose that demonstrates how to handle pop-up overflow by dynamically shifting the message list when a bubble near the screen edge is selected.

When a message bubble gets long-pressed near the top or bottom of the screen, the emoji reaction bar and action dialog (reply, copy, delete, etc.) often don't fit — they get clipped or pushed off-screen. Instead of repositioning the pop-ups themselves, this project shifts the underlying chat content just enough to make everything visible, keeping the interaction smooth and predictable.

Features:

- Long-press on any message to reveal an emoji reaction bar and action menu
- Dynamic vertical shift of the chat list — calculated per-selection based on available screen space above and below
- Animated transitions using `animateFloatAsState` (shift) and `Animatable` (spotlight fade)
- Spotlight overlay with a bubble cutout that matches the message shape and includes the sender's avatar
- Pure Jetpack Compose — no fragments, no manual layout hacks

##

![screen_record_gif](https://github.com/mostafa-tabasi/shifting-chats/blob/main/screenrecords/screen_record.gif)

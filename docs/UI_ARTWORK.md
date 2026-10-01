# Game artwork revision

The home screen now uses original illustrations generated with the built-in image generation tool. Reference screenshots informed composition and dimensional game styling; they are not packaged in the application.

Assets (copied unchanged from the generated outputs):
- `android/app/src/main/res/drawable-nodpi/local_mode_art_v2.png`: two friends at a Ludo table.
- `android/app/src/main/res/drawable-nodpi/online_mode_art_v2.png`: remote players, globe and Ludo board.

Both are decoded at half resolution for the small mode cards. The board and all interactive controls remain native Android views. Painted boards in the illustrations are decorative only.

Other visual changes: layered Ludo wordmark, gold rim and pressed state on buttons, gold circular navigation, player silhouettes, glossy pawn selection icons, larger board tokens and compact home composition.

## Generation prompts

### Local
Create a finished square mobile Ludo game mode illustration, original polished 3D cartoon game art. Two cheerful young friends, a brown-haired girl in a coral shirt and a black-haired boy in a turquoise shirt, playing a four-color Ludo board on a small wooden table. Chunky glossy red green yellow blue pawns and a white die on the table in foreground; expressive big eyes, appealing sculpted faces, rich saturated colors, soft studio lighting and bright specular highlights. Three-quarter view, close composition readable at 160 pixels wide. Characters and board occupy most of the square, table bottom near lower edge, keep heads inside frame. Bright cyan to royal-blue luminous background, subtle magical sparkles. Premium casual mobile board-game illustration, rendered toy-like dimensional materials, NOT flat vector, NOT wireframe, NOT generic geometric icons. No text, no lettering, no logo, no UI buttons, no border, no watermark.

### Online
Create a finished square mobile Ludo ONLINE multiplayer mode illustration, original polished 3D cartoon game art. A charming young brown-skinned male player with dark swept hair wearing a blue gaming headset and teal hoodie, and a smiling dark-haired female player in a violet gaming headset, each shown in floating glossy rounded avatar medallions in the upper left and upper right. Between them and in foreground a chunky small four-color red green yellow blue Ludo board in three-quarter perspective with shiny sculpted pawns and a white die. A luminous stylized blue globe and sweeping connecting arcs behind the board convey remote friends playing together. Large readable composition at 160px, sculpted toy-like faces and game pieces, saturated cyan royal-blue background with gentle stars, bold specular highlights, soft studio lighting, premium casual board-game app quality. Fill the square confidently without cutting off heads. NOT flat vector, NOT line diagram, NOT a UI mockup. No text or letters, no logo, no button, no frame, no watermark.

## Reference refinement

The follow-up pass uses a shared native TokenArt renderer for pearl-white pin bases and glazed coloured caps on both the board and colour selectors. Tokens are larger, dice faces retain opaque highlights, mode buttons have squarer shoulders, circular controls have bevelled gold rims, and the blue backdrop has soft star glows. These details are drawn in code; no new bitmap generation was needed.


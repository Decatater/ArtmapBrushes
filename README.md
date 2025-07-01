# Artmap Brushes
I created this plugin to apply a string to the custom_model_data nbt tag of any item renamed to a color of the artmap plugin. Will also remove the tag if the item is named something else or unnamed or, if the string on it isn't what its supposed to be, it sets it to whats in the config. Recent changes have made old options incompatible with 1.21.4 so I created this. Theoretically it can be used to apply any custom_model_data string to any item for any purpose. 
## Permissions and commands
#### Permissions
- `artmapbrushes.op`
    - allows all commands to be used, admin permission
- `artmapbrushes.reload`
    - allows usage of the reload command

#### Commands
- `/artmapbrushesreload`
    - Reloads the config to update NBT application without a restart
- `/artmapbrushespause`
    - Pauses application of strings on named items
- `/artmapbrushesresume`
    - Resumes application of strings

## This is only half the equation!
Your players will still need the resource pack to actually see the new textures, this plugin just applies the string the pack reads. I have made a x16 and x32 resource pack that takes all the hex values from [Gengyen](https://lospec.com/palette-list/fupery-artmap-shades-minecraft-120)'s palette and puts them on the tip of their relevant brushes so you can see the color you're painting with. [They can be found here](https://dekatater.com/artmaps/packs.html)
## Dependencies!
This plugin requires [NBTAPI](https://modrinth.com/plugin/nbtapi) to run. It handles the NBT.

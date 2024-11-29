This Bukkit (Paper) plugin adds two new sheep-related items (only accessible through commands) to the game.

Sheepish command (allows transformation into and back from being a sheep)
```
/give @s shears[custom_data={"sheep:sheepish":true},enchantment_glint_override=true,item_name="Sheepish"]
```

Sheeper command (increases raw sheep stats)
```
/give @s white_wool[custom_data={"sheep:sheeper":true},enchantment_glint_override=true,item_name="Sheeper"] 16
```

This plugin requires [NBT-API](https://modrinth.com/plugin/nbtapi/versions) to detect the Sheepish & Sheeper items.
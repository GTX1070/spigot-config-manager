# spigot-config-manager
A config manager for spigot plugins
## Project progress
- [X] Simple types
- [X] Lists
- [X] Enums & enum lists
- [X] Data classes with @Tree
- [X] Comments with @Comment
- [ ] Nested data classes
## How to use
```java

    @Tree 
    @Config(key = "config-key")
    @Comment(comment = "This is a comment!")
    private DataClass dataClass = new DataClass(1, 2, 3);
    
    @Config(key = "config-key.string")
    private String lol = "a";
    
    @Override
    public void onEnable() {
        FileConfiguration configuration = new YamlConfiguration();
        File file = new File(this.getDataFolder(), "config.yml");
        try {
            if(!file.exists())
                file.createNewFile();
            configuration.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        ConfigManager.INSTANCE.load(this, configuration, file);
    }
    

```

# DN-Storage

A powerful and feature-rich item storage plugin for Minecraft servers running Paper/Spigot. DN-Storage provides players with an intuitive GUI-based storage system that automatically categorizes items and offers advanced features like auto-pickup, multi-language support, and high-performance database operations.

## 📦 Spigot Resource

**Download and support the plugin on SpigotMC:**
- 🔗 [DN-Storage on SpigotMC](https://www.spigotmc.org/resources/dn-storage.131343/)

## ✨ Features

### Core Features
- **📦 GUI-Based Storage System**: Beautiful and intuitive graphical interface for managing items
- **🗂️ Automatic Item Categorization**: Items are automatically sorted into categories:
  - **Ore**: All ores, ingots, gems, and related materials
  - **Building Blocks**: Stone variants, bricks, concrete, glass, and construction materials
  - **Wood Blocks**: All wood types and their variants (logs, planks, stairs, slabs, etc.)
- **🔄 Auto-Pickup**: Automatically stores items when picked up (configurable per player)
- **🌍 Multi-Language Support**: Supports 10 languages:
  - Vietnamese (vi), English (en), Spanish (es), French (fr), German (de)
  - Chinese (zh), Japanese (ja), Korean (ko), Portuguese (pt), Russian (ru)
- **🔊 Sound Effects**: Configurable sound effects for GUI interactions
- **⚡ High Performance**: 
  - Async database operations (non-blocking)
  - Lazy loading for optimal memory usage
  - Batch operations for efficient item processing
  - Smart caching system

### Database Support
- **SQLite/H2**: Default embedded database (no setup required)
- **MySQL**: Full support for remote MySQL databases
- Easy migration between database types

### Commands
- `/storage` or `/kho` - Open the storage GUI
- `/storage reload` - Reload plugin configuration (requires `dnstorage.reload` permission)
- `/storage sort` - Automatically sort and store items from inventory

### Permissions
- `dnstorage.use` - Use the storage system (default: true)
- `dnstorage.reload` - Reload plugin configuration (default: op)
- `dnstorage.admin` - Admin access to storage system (default: op)

## 📋 Requirements

- **Minecraft Version**: 1.21+
- **Server Software**: Paper or Spigot
- **Java Version**: 21 or higher
- **API Version**: 1.21

## 🚀 Installation

1. Download the latest version from [SpigotMC](https://www.spigotmc.org/resources/dn-storage.131343/)
2. Place `DN-Storage-1.0.jar` in your server's `plugins` folder
3. Restart your server
4. Configure the plugin in `plugins/DN-Storage/config.yml`
5. Enjoy!

## ⚙️ Configuration

### Database Configuration

The plugin supports both SQLite (default) and MySQL:

```yaml
database:
  type: sqlite # or mysql
  
  # MySQL Configuration (only needed if type = mysql)
  mysql:
    host: localhost
    port: 3306
    database: dnstorage
    username: root
    password: ""
```

### Language Configuration

Set your preferred language in `config.yml`:

```yaml
language: en # Available: vi, en, es, fr, de, zh, ja, ko, pt, ru
```

### Sound Effects Configuration

Customize sound effects for better user experience:

```yaml
sounds:
  enabled: true
  volume: 1.0 # 0.0 - 1.0
  pitch: 1.0 # 0.5 - 2.0
  gui_open: "BLOCK_CHEST_OPEN"
  gui_close: "BLOCK_CHEST_CLOSE"
  item_add: "ENTITY_ITEM_PICKUP"
  item_remove: "ENTITY_ITEM_PICKUP"
```

## 📖 Usage

### Basic Usage

1. **Open Storage**: Use `/storage` or `/kho` to open the storage GUI
2. **Browse Categories**: Click on category icons to view items in that category
3. **Add Items**: Click the "Add All" button to automatically store all items from your inventory
4. **Remove Items**: Click on items in the storage to take them out
5. **Auto-Pickup**: Toggle auto-pickup in the GUI to automatically store items when picked up

### Advanced Features

- **Sort Inventory**: Use `/storage sort` to automatically organize and store items from your inventory
- **Reload Config**: Admins can use `/storage reload` to reload configuration without restarting the server

## 🏗️ Project Structure

```
DN-Storage/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/dnplugins/dNStorage/
│   │   │       ├── DNStorage.java          # Main plugin class
│   │   │       ├── commands/
│   │   │       │   └── CommandHandler.java # Command handling
│   │   │       ├── core/
│   │   │       │   ├── DatabaseManager.java    # Database operations
│   │   │       │   ├── StorageManager.java     # Storage logic
│   │   │       │   ├── LanguageManager.java    # Multi-language support
│   │   │       │   └── SoundManager.java       # Sound effects
│   │   │       ├── enums/
│   │   │       │   └── ItemCategory.java       # Item categorization
│   │   │       ├── gui/
│   │   │       │   └── StorageGUI.java         # GUI implementation
│   │   │       └── listeners/
│   │   │           └── AutoPickupListener.java # Auto-pickup feature
│   │   └── resources/
│   │       ├── config.yml                      # Main configuration
│   │       ├── plugin.yml                      # Plugin metadata
│   │       └── languages/                      # Language files
│   │           ├── en.yml
│   │           ├── vi.yml
│   │           └── ... (8 more languages)
└── pom.xml                                      # Maven configuration
```

## 🔧 Development

### Building from Source

1. Clone the repository
2. Ensure you have Maven installed
3. Run `maven clean package`
4. Find the compiled JAR in `target/DN-Storage-1.0.jar`

### Dependencies

- **Paper API**: 1.21.11-R0.1-SNAPSHOT
- **H2 Database**: 2.2.224 (for SQLite support)
- **MySQL Connector**: 8.2.0 (for MySQL support)

### Code Architecture

- **Async Operations**: All database write operations are asynchronous to prevent server lag
- **Lazy Loading**: Items are only loaded from the database when needed
- **Batch Operations**: Multiple items are processed together for efficiency
- **Caching**: Smart caching system reduces database queries

## 📊 Performance

DN-Storage is optimized for performance:

- ✅ **Non-blocking operations**: All database writes are async
- ✅ **Lazy loading**: Only loads data when needed
- ✅ **Batch processing**: Multiple items processed together
- ✅ **Smart caching**: Reduces database queries significantly
- ✅ **Minimal memory footprint**: Efficient data structures

## 🌐 Supported Languages

| Language | Code | Status |
|----------|------|--------|
| Vietnamese | `vi` | ✅ Complete |
| English | `en` | ✅ Complete |
| Spanish | `es` | ✅ Complete |
| French | `fr` | ✅ Complete |
| German | `de` | ✅ Complete |
| Chinese | `zh` | ✅ Complete |
| Japanese | `ja` | ✅ Complete |
| Korean | `ko` | ✅ Complete |
| Portuguese | `pt` | ✅ Complete |
| Russian | `ru` | ✅ Complete |

## 🐛 Troubleshooting

### Common Issues

**Issue**: Plugin doesn't load
- **Solution**: Ensure you're using Java 21+ and Paper/Spigot 1.21+

**Issue**: Database connection fails
- **Solution**: Check your database configuration in `config.yml`

**Issue**: Items not categorizing correctly
- **Solution**: Check if the item is in the supported categories (Ore, Building, Wood)

**Issue**: Auto-pickup not working
- **Solution**: Enable auto-pickup in the GUI or check player permissions

## 📝 Changelog

### Version 1.0.2
- ✅ Multi-language support (10 languages)
- ✅ Sound effects system
- ✅ Async database operations
- ✅ Lazy loading implementation
- ✅ Batch operations for better performance
- ✅ Auto-sort inventory command
- ✅ Reload command
- ✅ Auto-pickup settings persistence

### Version 1.0
- Initial release
- Basic GUI storage system
- Item categorization
- Auto-pickup feature

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License.

## 👤 Author

**devnguyen0111**
- Website: https://devnguyen.xyz
- SpigotMC: [DN-Storage Resource](https://www.spigotmc.org/resources/dn-storage.131343/)

## 🙏 Acknowledgments

- Thanks to all contributors and users
- Special thanks to the Paper/Spigot community
- Built with ❤️ for Minecraft server owners

## 📞 Support

For support, bug reports, or feature requests:
- Visit the [SpigotMC Resource Page](https://www.spigotmc.org/resources/dn-storage.131343/)
- Check the plugin configuration files
- Review server logs for error messages

---

**Made with ❤️ for the Minecraft community**


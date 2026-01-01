# DN-Storage

A powerful and feature-rich item storage plugin for Minecraft servers running Paper/Spigot 1.21+. This plugin provides players with an intuitive GUI-based storage system that automatically categorizes items into organized categories.

## 📋 Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Configuration](#configuration)
- [Commands](#commands)
- [Permissions](#permissions)
- [Database Support](#database-support)
- [Language Support](#language-support)
- [Usage](#usage)
- [Building from Source](#building-from-source)
- [Technical Details](#technical-details)
- [Support](#support)

## ✨ Features

### Core Features

- **📦 Categorized Storage System**: Automatically organizes items into three main categories:

  - **Ores**: All ores, ingots, gems, and related materials
  - **Building Blocks**: Stone variants, bricks, concrete, terracotta, glass, and more
  - **Wood Blocks**: All wood types and their variants (logs, planks, stairs, slabs, etc.)

- **🎨 Intuitive GUI Interface**: Beautiful and user-friendly graphical interface for managing stored items

  - Main menu with category selection
  - Category views with pagination support
  - Easy item deposit and withdrawal
  - Visual item display with quantities

- **🔄 Auto-Pickup System**: Automatically collects dropped items directly into storage

  - Toggle on/off per player
  - Only collects categorized items
  - Real-time notifications

- **📊 Inventory Management**:

  - Quick deposit all items from inventory to storage
  - Smart inventory sorting
  - Custom amount withdrawal
  - Automatic storage when inventory is full

- **💾 Database Support**:

  - SQLite (H2) for single-server setups
  - MySQL for multi-server networks
  - Automatic database initialization
  - Efficient data caching for performance

- **🌍 Multi-Language Support**:

  - 10+ languages included (English, Vietnamese, Spanish, French, German, Chinese, Japanese, Korean, Portuguese, Russian)
  - Easy to add custom languages
  - Per-server language configuration

- **🔊 Sound Effects**:

  - Configurable sound effects for GUI interactions
  - Customizable volume and pitch
  - Can be disabled if desired

- **⚡ Performance Optimized**:
  - Lazy loading system for categories
  - In-memory caching for fast access
  - Async database operations
  - Batch operations for bulk updates

## 📦 Requirements

- **Minecraft Version**: 1.21+
- **Server Software**: Paper (recommended) or Spigot
- **Java Version**: 21 or higher
- **Database**:
  - SQLite (H2) - Included, no setup required
  - MySQL 8.0+ (optional, for multi-server setups)

## 🚀 Installation

1. Download the latest `DN-Storage-2.0.jar` from the releases page
2. Place the JAR file in your server's `plugins` folder
3. Start or restart your server
4. The plugin will automatically generate configuration files
5. Configure the plugin in `plugins/DN-Storage/config.yml`
6. Reload the plugin using `/storage reload` or restart the server

## ⚙️ Configuration

### Main Configuration (`config.yml`)

```yaml
# Database Configuration
database:
  type: sqlite # sqlite or mysql

  # MySQL Configuration (only needed if type = mysql)
  mysql:
    host: localhost
    port: 3306
    database: dnstorage
    username: root
    password: ""

# Language Configuration
language: en # Language code (en, vi, es, fr, de, zh, ja, ko, pt, ru)

# Sound Effects Configuration
sounds:
  enabled: true
  volume: 1.0
  pitch: 1.0
  gui_open: "BLOCK_CHEST_OPEN"
  gui_close: "BLOCK_CHEST_CLOSE"
  item_add: "ENTITY_ITEM_PICKUP"
  item_remove: "ENTITY_ITEM_PICKUP"
```

### Database Types

#### SQLite (Default)

- No additional setup required
- Perfect for single-server setups
- Database file: `plugins/DN-Storage/storage.db`

#### MySQL

- Requires MySQL server setup
- Ideal for multi-server networks
- Configure connection details in `config.yml`

### Language Files

Language files are located in `plugins/DN-Storage/languages/`. You can:

- Edit existing language files to customize messages
- Add new language files (e.g., `nl.yml` for Dutch)
- Set the language in `config.yml`

## 📝 Commands

| Command           | Aliases           | Description                  | Permission         |
| ----------------- | ----------------- | ---------------------------- | ------------------ |
| `/storage`        | `/kho`, `/khochu` | Open the storage GUI         | `dnstorage.use`    |
| `/storage reload` | -                 | Reload plugin configuration  | `dnstorage.reload` |
| `/storage sort`   | -                 | Sort items in your inventory | `dnstorage.use`    |

### Command Examples

```
/storage          # Opens the storage GUI
/kho              # Opens the storage GUI (Vietnamese alias)
/storage reload   # Reloads config and language files
/storage sort     # Sorts items in your inventory
```

## 🔐 Permissions

| Permission         | Description                 | Default              |
| ------------------ | --------------------------- | -------------------- |
| `dnstorage.use`    | Use the storage system      | `true` (all players) |
| `dnstorage.reload` | Reload plugin configuration | `op`                 |
| `dnstorage.admin`  | Administrative permissions  | `op`                 |

### Permission Examples

```yaml
# Give all players access
permissions:
  dnstorage.use:
    default: true

# Only allow ops to reload
permissions:
  dnstorage.reload:
    default: op
```

## 💾 Database Support

### SQLite (H2 Database)

**Default database type**, perfect for most servers:

- Zero configuration required
- Database file stored locally
- Fast and reliable
- No external dependencies

### MySQL

**For multi-server networks**:

- Shared storage across multiple servers
- Centralized data management
- Requires MySQL server setup

**Setup Steps**:

1. Create a MySQL database
2. Configure connection details in `config.yml`
3. Set `database.type: mysql`
4. Restart the server

## 🌍 Language Support

The plugin includes translations for:

- 🇬🇧 **English** (en)
- 🇻🇳 **Vietnamese** (vi)
- 🇪🇸 **Spanish** (es)
- 🇫🇷 **French** (fr)
- 🇩🇪 **German** (de)
- 🇨🇳 **Chinese** (zh)
- 🇯🇵 **Japanese** (ja)
- 🇰🇷 **Korean** (ko)
- 🇵🇹 **Portuguese** (pt)
- 🇷🇺 **Russian** (ru)

### Adding Custom Languages

1. Copy an existing language file from `plugins/DN-Storage/languages/`
2. Rename it to your language code (e.g., `nl.yml` for Dutch)
3. Translate all messages
4. Set `language: nl` in `config.yml`
5. Reload the plugin

## 🎮 Usage

### Opening Storage

1. Use `/storage` or `/kho` command
2. The main GUI will open showing three categories
3. Click on a category to view items in that category

### Depositing Items

**Method 1: From GUI**

1. Open a category GUI
2. Click the "Deposit All" button
3. All items from your inventory in that category will be deposited

**Method 2: Auto-Pickup**

1. Enable auto-pickup from the main GUI
2. Pick up items from the ground
3. They will automatically be added to storage

**Method 3: Full Inventory**

- When your inventory is full and you pick up a categorized item, it will automatically go to storage

### Withdrawing Items

1. Open a category GUI
2. Click on an item to withdraw one stack
3. Right-click to withdraw a custom amount
4. Items will be added to your inventory

### Auto-Pickup

1. Open the main storage GUI
2. Click the "Auto Pickup" button to toggle
3. When enabled, picked up items will automatically go to storage
4. You'll receive notifications when items are auto-stored

### Sorting Inventory

1. Use `/storage sort` command
2. Items in your inventory will be sorted and organized
3. Items that can't be sorted will remain in place

## 🔨 Building from Source

### Prerequisites

- Java 21 or higher
- Maven 3.6+

### Build Steps

1. Clone the repository:

```bash
git clone <repository-url>
cd DN-Storage
```

2. Build the project:

```bash
maven clean package
```

3. The compiled JAR will be in `target/DN-Storage-2.0.jar`

### Development Setup

1. Import the project into your IDE (IntelliJ IDEA recommended)
2. Ensure Java 21 is configured
3. Maven dependencies will be automatically downloaded
4. Run the project using your IDE's build tools

## 🔧 Technical Details

### Architecture

The plugin follows a modular architecture:

```
org.dnplugins.dNStorage/
├── DNStorage.java              # Main plugin class
├── commands/
│   └── CommandHandler.java     # Command processing
├── core/
│   ├── DatabaseManager.java    # Database operations
│   ├── LanguageManager.java    # Language management
│   ├── SoundManager.java       # Sound effects
│   └── StorageManager.java    # Storage logic
├── enums/
│   └── ItemCategory.java      # Item categorization
├── gui/
│   └── StorageGUI.java        # GUI implementation
└── listeners/
    └── AutoPickupListener.java # Auto-pickup system
```

### Performance Features

- **Lazy Loading**: Categories are only loaded when accessed
- **Caching**: In-memory cache for frequently accessed data
- **Async Operations**: Database operations run asynchronously
- **Batch Updates**: Multiple items processed in batches

### Database Schema

```sql
-- Storage table
CREATE TABLE storage (
    player_uuid VARCHAR(36) NOT NULL,
    category VARCHAR(50) NOT NULL,
    material VARCHAR(100) NOT NULL,
    amount INT NOT NULL DEFAULT 0,
    PRIMARY KEY (player_uuid, category, material)
);

-- Auto-pickup settings
CREATE TABLE auto_pickup (
    player_uuid VARCHAR(36) PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT FALSE
);
```

### Supported Items

The plugin automatically categorizes hundreds of items:

- **Ores**: 20+ ore types and their processed forms
- **Building Blocks**: 100+ building materials
- **Wood Blocks**: 200+ wood-related items

## 📞 Support

- **Author**: devnguyen0111
- **Website**: https://devnguyen.xyz
- **Version**: 2.0
- **API Version**: 1.21

### Reporting Issues

If you encounter any bugs or have feature requests:

1. Check existing issues first
2. Create a detailed bug report including:
   - Minecraft version
   - Server software and version
   - Plugin version
   - Error logs
   - Steps to reproduce

### Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Built for the Minecraft community
- Uses Paper API for optimal performance
- H2 Database for embedded SQLite support
- MySQL Connector for database connectivity

---

**Made with ❤️ for Minecraft servers**

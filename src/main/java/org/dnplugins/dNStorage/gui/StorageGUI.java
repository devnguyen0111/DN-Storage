package org.dnplugins.dNStorage.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.dnplugins.dNStorage.core.LanguageManager;
import org.dnplugins.dNStorage.core.SoundManager;
import org.dnplugins.dNStorage.core.StorageManager;
import org.dnplugins.dNStorage.enums.ItemCategory;
import org.dnplugins.dNStorage.listeners.AutoPickupListener;

import java.util.*;

/**
 * Giao diện GUI chính cho kho chứa
 */
@SuppressWarnings("deprecation")
public class StorageGUI implements Listener {

    private final StorageManager storageManager;
    private final AutoPickupListener autoPickupListener;
    private final LanguageManager languageManager;
    private final SoundManager soundManager;
    private final JavaPlugin plugin;

    public StorageGUI(JavaPlugin plugin, StorageManager storageManager, AutoPickupListener autoPickupListener,
            LanguageManager languageManager, SoundManager soundManager) {
        this.plugin = plugin;
        this.storageManager = storageManager;
        this.autoPickupListener = autoPickupListener;
        this.languageManager = languageManager;
        this.soundManager = soundManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private String getMainTitle() {
        return languageManager.getMessage("gui.main.title");
    }

    private String getCategoryTitle(String category, int page, int total) {
        return languageManager.getMessage("gui.category.title")
                .replace("{category}", category)
                .replace("{page}", String.valueOf(page))
                .replace("{total}", String.valueOf(total));
    }

    private boolean isCategoryGUI(String title) {
        // Kiểm tra xem title có chứa pattern của category GUI không
        String categoryTitlePattern = languageManager.getMessage("gui.category.title");
        // Lấy phần prefix (trước {category})
        String prefix = categoryTitlePattern.substring(0, categoryTitlePattern.indexOf("{category}"));
        return title.startsWith(prefix.replace("&", "§"));
    }


    /**
     * Mở GUI chính với các danh mục
     */
    public void openMainGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, getMainTitle());

        // Đặt các nút danh mục
        gui.setItem(10, createCategoryButton(ItemCategory.Category.ORE, player));
        gui.setItem(13, createCategoryButton(ItemCategory.Category.BUILDING, player));
        gui.setItem(16, createCategoryButton(ItemCategory.Category.WOOD, player));

        // Nút tự động nhặt
        gui.setItem(4, createAutoPickupButton(player));

        // Nút sắp xếp inventory
        gui.setItem(8, createSortButton());

        // Nút đóng
        gui.setItem(22, createCloseButton());

        // Đặt các vật phẩm trang trí
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);

        for (int i = 0; i < 27; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, glass);
            }
        }

        soundManager.playGUIOpenSound(player);
        player.openInventory(gui);
    }

    /**
     * Mở GUI danh mục cụ thể (với lazy loading và async)
     */
    public void openCategoryGUI(Player player, ItemCategory.Category category) {
        // Hiển thị loading message
        player.sendMessage(languageManager.getMessage("message.storage.loading"));

        // Load items async (Lazy loading)
        storageManager.getCategoryItemsAsync(player.getUniqueId(), category, items -> {
            // Tính số trang cần thiết (45 items mỗi trang)
            int totalItems = items.size();
            int pages = (int) Math.ceil(totalItems / 45.0);
            if (pages == 0)
                pages = 1;

            openCategoryPage(player, category, items, 0, pages);
        });
    }

    /**
     * Mở một trang cụ thể của danh mục
     */
    private void openCategoryPage(Player player, ItemCategory.Category category,
            Map<Material, Integer> items, int page, int totalPages) {
        String categoryName = languageManager.getMessage("category." + category.name().toLowerCase());
        Inventory gui = Bukkit.createInventory(null, 54,
                getCategoryTitle(categoryName, page + 1, totalPages));

        // Sắp xếp vật phẩm theo tên
        List<Map.Entry<Material, Integer>> sortedItems = new ArrayList<>(items.entrySet());
        sortedItems.sort(Comparator.comparing(entry -> entry.getKey().name()));

        // Hiển thị 45 vật phẩm mỗi trang
        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, sortedItems.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<Material, Integer> entry = sortedItems.get(i);
            Material material = entry.getKey();
            int amount = entry.getValue();

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            // Đặt tên và lore
            String displayName = getMaterialDisplayName(material);
            meta.setDisplayName("§e" + displayName);

            List<String> lore = new ArrayList<>();
            lore.add(languageManager.getMessage("lore.item.amount").replace("{amount}", formatNumber(amount)));
            lore.add(" ");
            lore.add(languageManager.getMessage("lore.item.click_left"));
            lore.add(languageManager.getMessage("lore.item.click_right_item"));
            lore.add(languageManager.getMessage("lore.item.shift_click"));
            lore.add(" ");
            lore.add(languageManager.getMessage("lore.item.click_right_empty"));

            meta.setLore(lore);
            item.setItemMeta(meta);
            item.setAmount(Math.min(amount, material.getMaxStackSize()));

            gui.setItem(slot, item);
            slot++;
        }

        // Nút quay lại
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(languageManager.getMessage("button.back"));
        backButton.setItemMeta(backMeta);
        gui.setItem(45, backButton);

        // Nút thêm từ túi đồ
        ItemStack addFromInventoryButton = new ItemStack(Material.CHEST);
        ItemMeta addMeta = addFromInventoryButton.getItemMeta();
        addMeta.setDisplayName(languageManager.getMessage("button.add_from_inventory"));
        List<String> addLore = new ArrayList<>();
        String description = languageManager.getMessage("lore.add_from_inventory.description");
        // Split description thành các dòng (sử dụng \n)
        String[] descLines = description.split("\n");
        for (String line : descLines) {
            if (!line.trim().isEmpty()) {
                addLore.add(line.trim());
            }
        }
        addLore.add(" ");
        addLore.add(languageManager.getMessage("lore.add_from_inventory.click"));
        addMeta.setLore(addLore);
        addFromInventoryButton.setItemMeta(addMeta);
        gui.setItem(46, addFromInventoryButton);

        // Nút trang trước
        if (page > 0) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevButton.getItemMeta();
            prevMeta.setDisplayName(languageManager.getMessage("button.prev_page"));
            prevButton.setItemMeta(prevMeta);
            gui.setItem(48, prevButton);
        }

        // Nút trang sau
        if (page < totalPages - 1) {
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextButton.getItemMeta();
            nextMeta.setDisplayName(languageManager.getMessage("button.next_page"));
            nextButton.setItemMeta(nextMeta);
            gui.setItem(50, nextButton);
        }

        // Nút đóng
        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.setDisplayName(languageManager.getMessage("button.close"));
        closeButton.setItemMeta(closeMeta);
        gui.setItem(49, closeButton);

        // Đặt vật phẩm trang trí ở hàng cuối
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);

        for (int i = 45; i < 54; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, glass);
            }
        }

        player.openInventory(gui);
    }

    /**
     * Tạo nút danh mục (với lazy loading)
     */
    private ItemStack createCategoryButton(ItemCategory.Category category, Player player) {
        ItemStack button = new ItemStack(category.getIcon());
        ItemMeta meta = button.getItemMeta();

        String categoryName = languageManager.getMessage("category." + category.name().toLowerCase());
        meta.setDisplayName("§6§l" + categoryName);

        // Lazy loading - chỉ load khi cần hiển thị
        Map<Material, Integer> items = storageManager.getCategoryItems(player.getUniqueId(), category);
        int totalItems = items.size();
        int totalAmount = 0;
        for (int amount : items.values()) {
            totalAmount += amount;
        }

        List<String> lore = new ArrayList<>();
        lore.add(languageManager.getMessage("lore.category.item_count").replace("{count}", String.valueOf(totalItems)));
        lore.add(languageManager.getMessage("lore.category.total_amount").replace("{amount}",
                formatNumber(totalAmount)));
        lore.add(" ");
        lore.add(languageManager.getMessage("lore.category.click_to_open").replace("{category}", categoryName));

        meta.setLore(lore);
        button.setItemMeta(meta);

        return button;
    }

    /**
     * Tạo nút tự động nhặt
     */
    private ItemStack createAutoPickupButton(Player player) {
        boolean enabled = autoPickupListener.isAutoPickupEnabled(player.getUniqueId());
        ItemStack button = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = button.getItemMeta();

        meta.setDisplayName(enabled ? languageManager.getMessage("button.autopickup.on")
                : languageManager.getMessage("button.autopickup.off"));

        List<String> lore = new ArrayList<>();
        lore.add(languageManager.getMessage("lore.autopickup.description"));
        lore.add(" ");
        if (enabled) {
            lore.add(languageManager.getMessage("lore.autopickup.status.on"));
            lore.add(languageManager.getMessage("lore.autopickup.click_to_toggle")
                    .replace("{action}", languageManager.getMessage("lore.autopickup.toggle.on")));
        } else {
            lore.add(languageManager.getMessage("lore.autopickup.status.off"));
            lore.add(languageManager.getMessage("lore.autopickup.click_to_toggle")
                    .replace("{action}", languageManager.getMessage("lore.autopickup.toggle.off")));
        }

        meta.setLore(lore);
        button.setItemMeta(meta);
        return button;
    }

    /**
     * Tạo nút đóng
     */
    private ItemStack createCloseButton() {
        ItemStack button = new ItemStack(Material.BARRIER);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(languageManager.getMessage("button.close"));
        button.setItemMeta(meta);
        return button;
    }


    /**
     * Lấy tên hiển thị của vật phẩm
     */
    private String getMaterialDisplayName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0)
                result.append(" ");
            result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
        return result.toString();
    }

    /**
     * Định dạng số với dấu phẩy
     */
    private String formatNumber(int number) {
        return String.format("%,d", number);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Xử lý GUI chính
        if (title.equals(getMainTitle())) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            ItemStack clicked = event.getCurrentItem();

            // Kiểm tra nút danh mục
            if (clicked.getType() == ItemCategory.Category.ORE.getIcon()) {
                openCategoryGUI(player, ItemCategory.Category.ORE);
            } else if (clicked.getType() == ItemCategory.Category.BUILDING.getIcon()) {
                openCategoryGUI(player, ItemCategory.Category.BUILDING);
            } else if (clicked.getType() == ItemCategory.Category.WOOD.getIcon()) {
                openCategoryGUI(player, ItemCategory.Category.WOOD);
            } else if (clicked.getType() == Material.BARRIER) {
                player.closeInventory();
            } else if (clicked.getType() == Material.LIME_DYE || clicked.getType() == Material.GRAY_DYE) {
                // Nút tự động nhặt
                UUID playerId = player.getUniqueId();
                boolean currentState = autoPickupListener.isAutoPickupEnabled(playerId);
                autoPickupListener.setAutoPickup(playerId, !currentState);

                player.sendMessage(!currentState ? languageManager.getMessage("autopickup.enabled")
                        : languageManager.getMessage("autopickup.disabled"));

                // Cập nhật GUI
                openMainGUI(player);
            } else if (clicked.getType() == Material.HOPPER) {
                // Nút sắp xếp inventory
                sortPlayerInventory(player);
                // Cập nhật GUI
                openMainGUI(player);
            }
        }
        // Xử lý GUI danh mục
        else if (isCategoryGUI(title)) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();

            // Xử lý thêm vật phẩm vào kho (chuột phải)
            // Cho phép click chuột phải vào slot trống hoặc vào item trong GUI
            if (event.isRightClick() && event.getSlot() < 45) {
                // Lấy category hiện tại của GUI
                ItemCategory.Category currentCategory = getCategoryFromTitle(title);
                if (currentCategory == null) {
                    return;
                }

                // Kiểm tra vật phẩm trên cursor
                if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                    ItemStack item = event.getCursor();
                    Material material = item.getType();

                    // Kiểm tra vật phẩm có thuộc danh mục nào không
                    if (!ItemCategory.isCategorized(material)) {
                        player.sendMessage(languageManager.getMessage("message.item.not_storable"));
                        return;
                    }

                    // Kiểm tra category có khớp không
                    ItemCategory.Category itemCategory = ItemCategory.getCategory(material);
                    if (itemCategory != currentCategory) {
                        String categoryName = languageManager
                                .getMessage("category." + itemCategory.name().toLowerCase());
                        player.sendMessage(languageManager.getMessage("message.item.wrong_category")
                                .replace("{category}", categoryName));
                        return;
                    }

                    // Category đúng, cho phép đưa vào kho
                    int amount = item.getAmount();
                    int added = storageManager.addItem(player.getUniqueId(), material, amount);

                    if (added > 0) {
                        item.setAmount(0);
                        player.setItemOnCursor(null);
                        soundManager.playItemAddSound(player);
                        player.sendMessage(languageManager.getMessage("message.item.added")
                                .replace("{amount}", formatNumber(added))
                                .replace("{item}", getMaterialDisplayName(material)));

                        // Cập nhật GUI
                        openCategoryGUI(player, currentCategory);
                    } else {
                        player.sendMessage(languageManager.getMessage("message.item.not_storable"));
                    }
                    return;
                }

                // Nếu không có vật phẩm trên cursor
                if (clicked == null || clicked.getType() == Material.AIR) {
                    // Click vào slot trống: Tìm vật phẩm đầu tiên trong inventory thuộc danh mục
                    // hiện tại
                    for (ItemStack invItem : player.getInventory().getContents()) {
                        if (invItem != null && invItem.getType() != Material.AIR) {
                            ItemCategory.Category itemCategory = ItemCategory.getCategory(invItem.getType());
                            if (itemCategory == currentCategory) {
                                Material material = invItem.getType();
                                int amount = invItem.getAmount();
                                int added = storageManager.addItem(player.getUniqueId(), material, amount);

                                if (added > 0) {
                                    invItem.setAmount(0);
                                    soundManager.playItemAddSound(player);
                                    player.sendMessage(languageManager.getMessage("message.item.added")
                                            .replace("{amount}", formatNumber(added))
                                            .replace("{item}", getMaterialDisplayName(material)));
                                    openCategoryGUI(player, currentCategory);
                                }
                                break;
                            }
                        }
                    }
                } else {
                    // Click phải vào item trong GUI
                    Material material = clicked.getType();
                    ItemCategory.Category itemCategory = ItemCategory.getCategory(material);

                    if (itemCategory != null && itemCategory == currentCategory) {
                        // Click phải: Lấy 1 cái (async với lazy loading)
                        storageManager.getItemAmountAsync(player.getUniqueId(), material, availableAmount -> {
                            if (availableAmount <= 0) {
                                player.sendMessage(languageManager.getMessage("message.item.not_found"));
                                return;
                            }

                            // Lấy 1 cái
                            int taken = storageManager.removeItem(player.getUniqueId(), material, 1);

                            if (taken > 0) {
                                ItemStack item = new ItemStack(material, 1);
                                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);

                                if (!leftover.isEmpty()) {
                                    // Trả lại vào kho nếu inventory đầy
                                    storageManager.addItem(player.getUniqueId(), material, leftover.get(0).getAmount());
                                    player.sendMessage(languageManager.getMessage("message.inventory.full"));
                                } else {
                                    soundManager.playItemRemoveSound(player);
                                    player.sendMessage(languageManager.getMessage("message.item.removed")
                                            .replace("{amount}", formatNumber(1))
                                            .replace("{item}", getMaterialDisplayName(material)));
                                }

                                // Cập nhật GUI (async)
                                openCategoryGUI(player, currentCategory);
                            } else {
                                player.sendMessage(languageManager.getMessage("message.item.not_found"));
                            }
                        });
                    }
                }
                return;
            }

            // Nếu không có vật phẩm được click, không xử lý
            if (clicked == null || clicked.getType() == Material.AIR) {
                return;
            }

            // Nút quay lại
            if (clicked.getType() == Material.ARROW && event.getSlot() == 45) {
                event.setCancelled(true);
                openMainGUI(player);
                return;
            }

            // Nút thêm từ túi đồ
            if (clicked.getType() == Material.CHEST && event.getSlot() == 46) {
                event.setCancelled(true);

                // Parse category từ title
                ItemCategory.Category category = getCategoryFromTitle(title);
                if (category != null) {
                    addItemsFromInventory(player, category);
                }
                return;
            }

            // Nút đóng
            if (clicked.getType() == Material.BARRIER && event.getSlot() == 49) {
                event.setCancelled(true);
                player.closeInventory();
                return;
            }

            // Nút trang trước/sau
            if (clicked.getType() == Material.ARROW && (event.getSlot() == 48 || event.getSlot() == 50)) {
                event.setCancelled(true);

                // Parse category và page từ title
                ItemCategory.Category category = getCategoryFromTitle(title);
                if (category != null) {
                    // Parse page number
                    int parsedPage = 0;
                    try {
                        // Tìm pattern "(Page " hoặc "(Trang "
                        int pageStart = title.indexOf("(");
                        if (pageStart > 0) {
                            String pagePart = title.substring(pageStart);
                            // Tìm số trang
                            String[] parts = pagePart.split("/");
                            if (parts.length > 0) {
                                String pageStr = parts[0].replaceAll("[^0-9]", "");
                                if (!pageStr.isEmpty()) {
                                    parsedPage = Integer.parseInt(pageStr) - 1;
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Nếu không parse được, dùng page 0
                        parsedPage = 0;
                    }

                    final int currentPage = parsedPage; // Final để sử dụng trong lambda
                    final int slot = event.getSlot(); // Final để sử dụng trong lambda

                    // Load items async (Lazy loading)
                    storageManager.getCategoryItemsAsync(player.getUniqueId(), category, items -> {
                        int totalItems = items.size();
                        int totalPages = (int) Math.ceil(totalItems / 45.0);
                        if (totalPages == 0)
                            totalPages = 1;

                        int newPage = currentPage;
                        if (slot == 48) {
                            // Trang trước
                            newPage = Math.max(0, currentPage - 1);
                        } else if (slot == 50) {
                            // Trang sau
                            newPage = Math.min(totalPages - 1, currentPage + 1);
                        }

                        openCategoryPage(player, category, items, newPage, totalPages);
                    });
                }
                return;
            }

            // Lấy vật phẩm từ kho (chuột trái)
            if (clicked != null && event.getSlot() < 45 && clicked.getType() != Material.GRAY_STAINED_GLASS_PANE
                    && !event.isRightClick()) {
                // Chỉ xử lý khi click trái (không phải click phải)
                // Nếu có cursor item, không xử lý (để tránh xung đột)
                if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                    return;
                }

                Material material = clicked.getType();
                ItemCategory.Category category = ItemCategory.getCategory(material);

                if (category == null) {
                    return;
                }

                // Kiểm tra vật phẩm có trong kho không (async với lazy loading)
                storageManager.getItemAmountAsync(player.getUniqueId(), material, availableAmount -> {
                    if (availableAmount <= 0) {
                        player.sendMessage(languageManager.getMessage("message.item.not_found"));
                        return;
                    }

                    int amountToTake;
                    if (event.isShiftClick()) {
                        // Shift + Click trái: Lấy tất cả
                        amountToTake = availableAmount;
                    } else {
                        // Click trái: Lấy 1 stack
                        amountToTake = Math.min(material.getMaxStackSize(), availableAmount);
                    }

                    int taken = storageManager.removeItem(player.getUniqueId(), material, amountToTake);

                    if (taken > 0) {
                        ItemStack item = new ItemStack(material, taken);
                        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);

                        if (!leftover.isEmpty()) {
                            // Trả lại vào kho nếu inventory đầy
                            storageManager.addItem(player.getUniqueId(), material, leftover.get(0).getAmount());
                            player.sendMessage(languageManager.getMessage("message.inventory.full"));
                        } else {
                            soundManager.playItemRemoveSound(player);
                            player.sendMessage(languageManager.getMessage("message.item.removed")
                                    .replace("{amount}", formatNumber(taken))
                                    .replace("{item}", getMaterialDisplayName(material)));
                        }

                        // Cập nhật GUI (async)
                        openCategoryGUI(player, category);
                    } else {
                        player.sendMessage(languageManager.getMessage("message.item.not_found"));
                    }
                });
            }
        }
    }

    /**
     * Lấy category từ title của GUI
     */
    private ItemCategory.Category getCategoryFromTitle(String title) {
        if (!isCategoryGUI(title)) {
            return null;
        }

        // Parse category name từ title
        // Format: "§6§lKHO CHỨA - {category} §7(Trang {page}/{total})"
        String categoryTitlePattern = languageManager.getMessage("gui.category.title");
        String prefix = categoryTitlePattern.substring(0, categoryTitlePattern.indexOf("{category}"));
        prefix = prefix.replace("&", "§");

        String categoryName = title.replace(prefix, "");
        // Tìm vị trí của " §7(" hoặc " (Page " tùy ngôn ngữ
        int pageIndex = categoryName.indexOf(" §7(");
        if (pageIndex < 0) {
            pageIndex = categoryName.indexOf(" (Page ");
        }
        if (pageIndex > 0) {
            categoryName = categoryName.substring(0, pageIndex);
        }

        // So sánh với tên category trong các ngôn ngữ
        for (ItemCategory.Category cat : ItemCategory.Category.values()) {
            String viName = languageManager.getMessage("category." + cat.name().toLowerCase());
            if (categoryName.equals(viName) || categoryName.equals(cat.getDisplayName())) {
                return cat;
            }
        }

        return null;
    }

    /**
     * Thêm tất cả vật phẩm từ inventory vào kho theo danh mục (sử dụng batch
     * operations)
     */
    private void addItemsFromInventory(Player player, ItemCategory.Category category) {
        // Thu thập tất cả items cần thêm (Batch operations)
        Map<Material, Integer> itemsToAdd = new HashMap<>();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            Material material = item.getType();
            ItemCategory.Category itemCategory = ItemCategory.getCategory(material);

            if (itemCategory == category) {
                int amount = item.getAmount();
                itemsToAdd.put(material, itemsToAdd.getOrDefault(material, 0) + amount);
            }
        }

        if (itemsToAdd.isEmpty()) {
            String categoryName = languageManager.getMessage("category." + category.name().toLowerCase());
            player.sendMessage(languageManager.getMessage("message.items.none_in_inventory")
                    .replace("{category}", categoryName));
            return;
        }

        // Sử dụng batch operations để thêm tất cả items cùng lúc
        storageManager.batchAddItems(player.getUniqueId(), itemsToAdd);

        // Xóa items khỏi inventory
        int totalAdded = 0;
        int itemsCount = itemsToAdd.size();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            Material material = item.getType();
            if (itemsToAdd.containsKey(material)) {
                totalAdded += item.getAmount();
                item.setAmount(0);
            }
        }

        String categoryName = languageManager.getMessage("category." + category.name().toLowerCase());
        if (totalAdded > 0) {
            soundManager.playItemAddSound(player);
        }
        player.sendMessage(languageManager.getMessage("message.items.added_from_inventory")
                .replace("{total}", formatNumber(totalAdded))
                .replace("{count}", String.valueOf(itemsCount))
                .replace("{category}", categoryName));

        // Cập nhật GUI (async)
        openCategoryGUI(player, category);
    }

    /**
     * Sắp xếp tất cả vật phẩm trong inventory
     * Sắp xếp theo: số lượng giảm dần -> tên bảng chữ cái -> item ID tăng dần
     */
    public void sortPlayerInventory(Player player) {
        // Thu thập và gộp các items cùng loại từ inventory
        Map<Material, Integer> itemsMap = new HashMap<>();
        Map<Material, ItemStack> itemStackMap = new HashMap<>();
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                Material material = item.getType();
                itemsMap.put(material, itemsMap.getOrDefault(material, 0) + item.getAmount());
                // Lưu một ItemStack mẫu (với metadata) để giữ nguyên thông tin
                if (!itemStackMap.containsKey(material)) {
                    itemStackMap.put(material, item.clone());
                }
            }
        }

        if (itemsMap.isEmpty()) {
            player.sendMessage(languageManager.getMessage("message.sort.no_items"));
            return;
        }

        // Chuyển đổi sang List để sắp xếp
        List<Map.Entry<Material, Integer>> itemsList = new ArrayList<>(itemsMap.entrySet());

        // Sắp xếp items theo thứ tự:
        // 1. Số lượng giảm dần (descending order of quantity)
        // 2. Tên bảng chữ cái (alphabetical order)
        // 3. Item ID tăng dần (ascending order of item ID)
        itemsList.sort((entry1, entry2) -> {
            Material mat1 = entry1.getKey();
            Material mat2 = entry2.getKey();
            int amount1 = entry1.getValue();
            int amount2 = entry2.getValue();

            // So sánh theo số lượng (giảm dần)
            int amountCompare = Integer.compare(amount2, amount1);
            if (amountCompare != 0) {
                return amountCompare;
            }

            // Nếu số lượng bằng nhau, so sánh theo tên (bảng chữ cái)
            String name1 = getMaterialDisplayName(mat1);
            String name2 = getMaterialDisplayName(mat2);
            int nameCompare = name1.compareToIgnoreCase(name2);
            if (nameCompare != 0) {
                return nameCompare;
            }

            // Nếu tên giống nhau, so sánh theo item ID (tăng dần)
            return mat1.name().compareTo(mat2.name());
        });

        // Xóa tất cả items khỏi inventory
        player.getInventory().clear();

        // Đặt lại items đã sắp xếp vào inventory
        int slot = 0;
        for (Map.Entry<Material, Integer> entry : itemsList) {
            Material material = entry.getKey();
            int totalAmount = entry.getValue();
            ItemStack template = itemStackMap.get(material);
            
            // Chia thành các stack (tối đa maxStackSize mỗi stack)
            int maxStackSize = material.getMaxStackSize();
            while (totalAmount > 0 && slot < player.getInventory().getSize()) {
                ItemStack stack = template.clone();
                int stackAmount = Math.min(totalAmount, maxStackSize);
                stack.setAmount(stackAmount);
                player.getInventory().setItem(slot, stack);
                totalAmount -= stackAmount;
                slot++;
            }
            
            // Nếu còn items nhưng inventory đầy, thả xuống đất
            if (totalAmount > 0) {
                ItemStack leftover = template.clone();
                leftover.setAmount(totalAmount);
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }

        soundManager.playItemAddSound(player);
        player.sendMessage(languageManager.getMessage("message.sort.success")
                .replace("{total}", formatNumber(itemsList.size())));
    }

    /**
     * Tạo nút sắp xếp inventory
     */
    private ItemStack createSortButton() {
        ItemStack button = new ItemStack(Material.HOPPER);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(languageManager.getMessage("button.sort"));
        List<String> lore = new ArrayList<>();
        lore.add(languageManager.getMessage("lore.sort.description"));
        lore.add(" ");
        lore.add(languageManager.getMessage("lore.sort.click"));
        meta.setLore(lore);
        button.setItemMeta(meta);
        return button;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            String title = event.getView().getTitle();
            
            // Chỉ phát sound khi đóng GUI chính hoặc category GUI
            if (title.equals(getMainTitle()) || isCategoryGUI(title)) {
                soundManager.playGUICloseSound(player);
            }
        }
    }
}

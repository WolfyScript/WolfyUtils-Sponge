package com.wolfyscript.utilities.sponge.world.items;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wolfyscript.utilities.WolfyUtils;
import com.wolfyscript.utilities.sponge.adapters.ItemStackImpl;
import com.wolfyscript.utilities.world.items.ItemStackConfig;
import com.wolfyscript.utilities.eval.context.EvalContext;
import com.wolfyscript.utilities.eval.operator.BoolOperatorConst;
import com.wolfyscript.utilities.eval.value_provider.*;
import com.wolfyscript.utilities.nbt.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.DataManipulator;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class SpongeItemStackConfig extends ItemStackConfig {

    private final Set<String> HANDLED_NBT_TAGS = Set.of("display.Name", "display.Lore", "CustomModelData", "Damage", "Enchantments");

    @JsonCreator
    public SpongeItemStackConfig(@JacksonInject WolfyUtils wolfyUtils, @JsonProperty("itemId") String itemId) {
        super(wolfyUtils, itemId);
    }

    public SpongeItemStackConfig(WolfyUtils wolfyUtils, ItemStack stack) {
        super(wolfyUtils, ItemTypes.registry().findValueKey(stack.type()).orElse(ResourceKey.minecraft("air")).toString());

        // Read from ItemStack
        this.amount = new ValueProviderIntegerConst(wolfyUtils, stack.quantity());

        MiniMessage miniMsg = wolfyUtils.getChat().getMiniMessage();

        stack.get(Keys.DISPLAY_NAME).ifPresent(component -> {
            this.name = new ValueProviderStringConst(wolfyUtils, miniMsg.serialize(component));
        });

        stack.get(Keys.LORE).ifPresent(components -> {
            this.lore = components.stream().map(component -> new ValueProviderStringConst(wolfyUtils, miniMsg.serialize(component))).toList();
        });

        this.unbreakable = new BoolOperatorConst(wolfyUtils, stack.get(Keys.IS_UNBREAKABLE).orElse(false));
        this.customModelData = new ValueProviderIntegerConst(wolfyUtils, stack.get(Keys.CUSTOM_MODEL_DATA).orElse(0));

        stack.get(Keys.APPLIED_ENCHANTMENTS).ifPresentOrElse(enchantments -> this.enchants = enchantments.stream().collect(Collectors.toMap(enchantment -> EnchantmentTypes.registry().valueKey(enchantment.type()).toString(), enchantment -> new ValueProviderIntegerConst(wolfyUtils, enchantment.level()))), () -> this.enchants = new HashMap<>());

        this.nbt = new NBTTagConfigCompound(wolfyUtils, null); // TODO: Read NBT! stack.getType() != Material.AIR && stack.getAmount() > 0 ? readFromItemStack(stack.toContainer(), "", null) : new NBTTagConfigCompound(wolfyUtils, null);
    }

    @Override
    public ItemStackImpl constructItemStack() {
        return constructItemStack(new EvalContext());
    }

    @Override
    public ItemStackImpl constructItemStack(EvalContext context) {
        return constructItemStack(context, wolfyUtils.getChat().getMiniMessage(), TagResolver.empty());
    }

    @Override
    public ItemStackImpl constructItemStack(EvalContext context, MiniMessage miniMsg, TagResolver tagResolvers) {
        return new ItemStackImpl(wolfyUtils, ItemTypes.registry().findValue(ResourceKey.resolve(itemId)).map(itemType -> {
            ItemStack.Builder builder = ItemStack.builder()
                    .itemType(itemType)
                    .quantity(amount.getValue(context));

            // TODO: Apply NBT

            String nameVal = name == null ? null : name.getValue(context);
            // Apply handled settings
            if (nameVal != null) {
                builder.add(Keys.CUSTOM_NAME, miniMsg.deserialize(nameVal, tagResolvers));
            }
            if (lore != null && !lore.isEmpty()) {
                builder.add(Keys.LORE, lore.stream().map(line -> miniMsg.deserialize(line.getValue(context), tagResolvers)).toList());
            }

            List<Enchantment> enchantments = new ArrayList<>();
            for (var entry : enchants.entrySet()) {
                var key = ResourceKey.resolve(entry.getKey());
                EnchantmentTypes.registry().findValue(key).ifPresent(enchantmentType -> enchantments.add(Enchantment.of(enchantmentType, entry.getValue().getValue(context))));
            }
            builder.add(Keys.APPLIED_ENCHANTMENTS, enchantments);

            if (customModelData != null) {
                builder.add(Keys.CUSTOM_MODEL_DATA, customModelData.getValue(context));
            }
            if (unbreakable != null) {
                builder.add(Keys.IS_UNBREAKABLE, unbreakable.evaluate(context));
            }

            return builder.build();
        }).orElse(null));
    }

    private NBTTagConfigCompound readFromItemStack(DataContainer currentCompound, String path, NBTTagConfig parent) {
        NBTTagConfigCompound configCompound = new NBTTagConfigCompound(wolfyUtils, parent);
        Map<String, NBTTagConfig> children = new HashMap<>();
        for (DataQuery key : currentCompound.keys(true)) {
            // TODO
            /*
            String childPath = path.isEmpty() ? key : (path + "." + key);
            if (HANDLED_NBT_TAGS.contains(childPath)) {
                // Skip already handled NBT Tags, so they are not both in common and NBT settings!
                continue;
            }
            NBTTagConfig childConfig = switch (currentCompound.getType(key)) {
                case NBTTagCompound -> {
                    NBTTagConfigCompound readConfigCompound = readFromItemStack(currentCompound.getCompound(key), childPath, configCompound);
                    if (readConfigCompound.getChildren().isEmpty()) {
                        yield null;
                    }
                    yield readConfigCompound;
                }
                case NBTTagList -> switch (currentCompound.getListType(key)) {
                    case NBTTagCompound -> {
                        NBTTagConfigListCompound compoundConfigList = new NBTTagConfigListCompound(wolfyUtils, parent, List.of());
                        ReadableNBTList<ReadWriteNBT> compoundList = currentCompound.getCompoundList(key);
                        List<NBTTagConfigCompound> elements = new ArrayList<>();
                        int index = 0;
                        for (ReadWriteNBT listCompound : compoundList) {
                            elements.add(readFromItemStack(listCompound, childPath + "." + index, compoundConfigList));
                            index++;
                        }
                        compoundConfigList.setValues(elements);
                        yield compoundConfigList;
                    }
                    case NBTTagInt ->
                            readPrimitiveList(currentCompound.getIntegerList(key), new NBTTagConfigListInt(wolfyUtils, configCompound, new ArrayList<>()), (listInt, integer) -> new NBTTagConfigInt(wolfyUtils, listInt, new ValueProviderIntegerConst(wolfyUtils, integer)));
                    case NBTTagIntArray ->
                            readPrimitiveList(currentCompound.getIntArrayList(key), new NBTTagConfigListIntArray(wolfyUtils, configCompound, new ArrayList<>()), (listIntArray, intArray) -> new NBTTagConfigIntArray(wolfyUtils, listIntArray, new ValueProviderIntArrayConst(wolfyUtils, intArray)));
                    case NBTTagLong ->
                            readPrimitiveList(currentCompound.getLongList(key), new NBTTagConfigListLong(wolfyUtils, configCompound, new ArrayList<>()), (listConfig, aLong) -> new NBTTagConfigLong(wolfyUtils, listConfig, new ValueProviderLongConst(wolfyUtils, aLong)));
                    case NBTTagFloat ->
                            readPrimitiveList(currentCompound.getFloatList(key), new NBTTagConfigListFloat(wolfyUtils, configCompound, new ArrayList<>()), (listConfig, aFloat) -> new NBTTagConfigFloat(wolfyUtils, listConfig, new ValueProviderFloatConst(wolfyUtils, aFloat)));
                    case NBTTagDouble ->
                            readPrimitiveList(currentCompound.getDoubleList(key), new NBTTagConfigListDouble(wolfyUtils, configCompound, new ArrayList<>()), (listConfig, aDouble) -> new NBTTagConfigDouble(wolfyUtils, listConfig, new ValueProviderDoubleConst(wolfyUtils, aDouble)));
                    case NBTTagString ->
                            readPrimitiveList(currentCompound.getStringList(key), new NBTTagConfigListString(wolfyUtils, configCompound, new ArrayList<>()), (listConfig, aString) -> new NBTTagConfigString(wolfyUtils, listConfig, new ValueProviderStringConst(wolfyUtils, aString)));
                    default -> null;
                };
                case NBTTagByte ->
                        new NBTTagConfigByte(wolfyUtils, configCompound, new ValueProviderByteConst(wolfyUtils, currentCompound.getByte(key)));
                case NBTTagByteArray ->
                        new NBTTagConfigByteArray(wolfyUtils, configCompound, new ValueProviderByteArrayConst(wolfyUtils, currentCompound.getByteArray(key)));
                case NBTTagShort ->
                        new NBTTagConfigShort(wolfyUtils, configCompound, new ValueProviderShortConst(wolfyUtils, currentCompound.getShort(key)));
                case NBTTagInt ->
                        new NBTTagConfigInt(wolfyUtils, configCompound, new ValueProviderIntegerConst(wolfyUtils, currentCompound.getInteger(key)));
                case NBTTagIntArray ->
                        new NBTTagConfigIntArray(wolfyUtils, configCompound, new ValueProviderIntArrayConst(wolfyUtils, currentCompound.getIntArray(key)));
                case NBTTagLong ->
                        new NBTTagConfigLong(wolfyUtils, configCompound, new ValueProviderLongConst(wolfyUtils, currentCompound.getLong(key)));
                case NBTTagFloat ->
                        new NBTTagConfigFloat(wolfyUtils, configCompound, new ValueProviderFloatConst(wolfyUtils, currentCompound.getFloat(key)));
                case NBTTagDouble ->
                        new NBTTagConfigDouble(wolfyUtils, configCompound, new ValueProviderDoubleConst(wolfyUtils, currentCompound.getDouble(key)));
                case NBTTagString ->
                        new NBTTagConfigString(wolfyUtils, configCompound, new ValueProviderStringConst(wolfyUtils, currentCompound.getString(key)));
                default -> null;
            };
            if (childConfig != null) {
                children.put(key, childConfig);
            }
             */
        }
        configCompound.setChildren(children);
        return configCompound;
    }

    @Override
    public String toString() {
        return "BukkitItemStackConfig{" +
                "itemId='" + itemId + '\'' +
                ", name=" + name +
                ", lore=" + lore +
                ", amount=" + amount +
                ", repairCost=" + repairCost +
                ", damage=" + damage +
                ", unbreakable=" + unbreakable +
                ", customModelData=" + customModelData +
                ", enchants=" + enchants +
                ", nbt=" + nbt +
                "} ";
    }
}

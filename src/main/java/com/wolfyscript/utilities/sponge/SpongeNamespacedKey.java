package com.wolfyscript.utilities.sponge;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.base.Preconditions;
import com.wolfyscript.utilities.NamespacedKey;
import com.wolfyscript.utilities.WolfyUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.ResourceKey;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public class SpongeNamespacedKey implements NamespacedKey, Comparable<SpongeNamespacedKey> {

    public static final String WOLFYUTILITIES = "wolfyutilities";

    @JsonIgnore
    private static final Pattern VALID_NAMESPACE = Pattern.compile("[a-z0-9._-]+");
    @JsonIgnore
    private static final Pattern VALID_KEY = Pattern.compile("[a-z0-9/._-]+");
    private final String namespace;
    private final Key key;

    /**
     * Creates a NamespacedKey with a custom namespace and key.<br>
     * This does not uniquely link the resource to your plugin! (Except the namespace is exactly the same as your plugins' lowercase name)<br>
     * <b>Only for internal use. Must be converted to a namespaced key with your plugins' (lowercase) name as the namespace, if you register data in WolfyUtilities!</b>
     *
     * @param namespace The namespace, that fits the pattern [a-z0-9._-]
     * @param key       The key that fits the pattern [a-z0-9/._-]
     */
    public SpongeNamespacedKey(String namespace, String key) {
        Preconditions.checkArgument(namespace != null && VALID_NAMESPACE.matcher(namespace).matches(), "Invalid namespace. Must be [a-z0-9._-]: %s", namespace);
        this.key = new Key(key.toLowerCase(Locale.ROOT));
        this.namespace = namespace;
        var string = this.toString();
        Preconditions.checkArgument(string.length() < 256, "NamespacedKey must be less than 256 characters", string);
    }

    /**
     * Creates a new NamespacedKey with the plugins' (lowercase) name.
     *
     * @param api    The plugin that this data belongs to
     * @param key    The key that fits the pattern [a-z0-9/._-]
     */
    public SpongeNamespacedKey(@NotNull WolfyUtils api, @NotNull String key) {
        Preconditions.checkArgument(api != null, "Plugin cannot be null");
        Preconditions.checkArgument(key != null, "Key cannot be null");
        this.namespace = api.getName().toLowerCase(Locale.ROOT).replace(" ", "_");
        Preconditions.checkArgument(VALID_NAMESPACE.matcher(this.namespace).matches(), "Invalid namespace. Must be [a-z0-9._-]: %s", this.namespace);
        this.key = new Key(key.toLowerCase(Locale.ROOT));
        String string = this.toString();
        Preconditions.checkArgument(string.length() < 256, "NamespacedKey must be less than 256 characters (%s)", string);
    }

    /**
     * Gets the namespace of this object.
     *
     * @return The namespace.
     */
    @NotNull
    public String getNamespace() {
        return this.namespace;
    }

    /**
     * Gets the key of this object as a String.
     *
     * @return The key.
     */
    @NotNull
    public String getKey() {
        return this.key.toString();
    }

    /**
     * Gets the key part of this NamespacedKey.
     *
     * @return The key part.
     * @since 3.16.1.0
     */
    @ApiStatus.AvailableSince(value = "3.16.1.0")
    public Key getKeyComponent() {
        return this.key;
    }

    /**
     * @param namespaceKey The String with a column.
     * @return The NamespacedKey of the String or null, if the String doesn't contain a ":".
     */
    @Nullable
    public static SpongeNamespacedKey of(@Nullable String namespaceKey) {
        if (namespaceKey == null || namespaceKey.isEmpty()) return null;
        String[] parts = namespaceKey.split(":", 2);
        if (parts.length == 0) return null;
        if (parts.length > 1) {
            return new SpongeNamespacedKey(parts[0].toLowerCase(Locale.ROOT), parts[1].toLowerCase(Locale.ROOT));
        } else {
            return wolfyutilties(parts[0]);
        }
    }

    public static SpongeNamespacedKey of(ResourceKey key) {
        return new SpongeNamespacedKey(key.namespace(), key.value());
    }

    public static SpongeNamespacedKey wolfyutilties(String key) {
        return new SpongeNamespacedKey(SpongeNamespacedKey.WOLFYUTILITIES, key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SpongeNamespacedKey that)) return false;
        return Objects.equals(namespace, that.namespace) &&
               Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, key);
    }

    @JsonValue
    @Override
    public String toString() {
        return toString(":");
    }

    public String toString(String split) {
        if (split == null || split.isEmpty()) {
            split = ":";
        }
        return getNamespace() + split + getKey();
    }

    @Override
    public int compareTo(@NotNull SpongeNamespacedKey namespacedKey) {
        int namespaceDifference = getNamespace().compareTo(namespacedKey.getNamespace());
        return namespaceDifference == 0 ? getKey().compareTo(namespacedKey.getKey()) : namespaceDifference;
    }

    /**
     * Represents the key part of the NamespacedKey.
     * The idea behind this component is to make it easier to manage folders specified in the key and provide util methods for it.<br>
     * Such as
     * <pre>
     *     "folder/sub_folder/another/file"</pre>
     * is converted into
     * <pre>
     *     folder:  "folder/sub_folder/another"
     *     key:     "file"
     * </pre>
     */
    @ApiStatus.AvailableSince(value = "3.16.1.0")
    public static final class Key implements NamespacedKey.Key {

        private final String folder;
        private final String object;

        private Key(String keyString) {
            Preconditions.checkArgument(VALID_KEY.matcher(keyString).matches(), "Invalid key. Must be [a-z0-9/._-]: %s", keyString);
            if (keyString.contains("/")) {
                String[] args = keyString.split("/(?!.*/)");
                if (args.length > 1) {
                    this.folder = args[0];
                    this.object = args[1];
                    return;
                } else if (args.length == 1) {
                    this.folder = "";
                    this.object = args[0];
                    return;
                }
            }
            this.object = keyString;
            this.folder = "";
        }

        /**
         * Gets the folder of the key component.
         *
         * @return The folder part of this component.
         */
        public String getFolder() {
            return folder;
        }

        /**
         * Gets the object part of this key component.
         *
         * @return The object of this component.
         */
        public String getObject() {
            return object;
        }

        /**
         * Gets the root folder of the folder part of this component.<br>
         * In case the folder doesn't exist it returns an empty String.<br>
         * If the folder part exists, but doesn't contain a "/", then the folder part will be returned as is.<br>
         *
         * @return The root folder; otherwise, an empty String if the key has no folder, or the folder itself if the folder contains no subfolder.
         */
        public String getRoot() {
            if (folder.isBlank()) return "";
            return folder.contains("/") ? folder.substring(0, folder.indexOf("/")) : folder;
        }

        /**
         * Gets the string representation of the folder and object part of this component.<br>
         * The folder and object are concat using "/" in between.<br>
         * e.g. <code>folder/subfolder object -> folder/subfolder/object</code><br>
         * If no folder is available it will just return the object part.
         *
         * @return The folder and key part separated by a "/".
         */
        @Override
        public String toString() {
            return toString("/");
        }

        /**
         * Gets the string representation of the folder and object part of this component.<br>
         * The folder and object are concat using a specified separator in between.<br>
         * e.g. <code>separator=":" folder/subfolder object -> folder/subfolder:object</code><br>
         * If no folder is available it will just return the object part.
         *
         * @param separator The separator between folder and object.
         * @return The folder and key part separated by the specified separator.
         */
        public String toString(String separator) {
            return toString(separator, false);
        }

        /**
         * Gets the string representation of the folder and object part of this component.<br>
         * The folder and object are concat using a specified separator in between.<br>
         * <code>forceSeparator=true|false, separator=":" </code><br>
         * <code>folder/subfolder object -> folder/subfolder:object</code><br>
         * <br>
         * <code>forceSeparator=true, separator="/"</code><br>
         * <code>  object -> /object</code><br>
         *
         * @param separator The separator between folder and object.
         * @param forceSeparator Add the separator in the front of the object, even if no folder exists.
         * @return The folder and key part separated by the specified separator.
         */
        public String toString(String separator, boolean forceSeparator) {
            if (separator == null || separator.isEmpty()) {
                separator = "/";
            }
            return folder.isBlank() ? ((forceSeparator ? separator : "").concat(object)) : (folder.concat(separator).concat(object));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key that = (Key) o;
            return Objects.equals(folder, that.folder) && Objects.equals(object, that.object);
        }

        @Override
        public int hashCode() {
            return Objects.hash(folder, object);
        }
    }

    static class Deserializer extends JsonDeserializer<SpongeNamespacedKey> {

        @Override
        public SpongeNamespacedKey deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            return SpongeNamespacedKey.of(jsonParser.readValueAs(String.class));
        }
    }

    static class KeyDeserializer extends com.fasterxml.jackson.databind.KeyDeserializer {

        @Override
        public SpongeNamespacedKey deserializeKey(String s, DeserializationContext deserializationContext) throws IOException {
            return SpongeNamespacedKey.of(s);
        }
    }

}

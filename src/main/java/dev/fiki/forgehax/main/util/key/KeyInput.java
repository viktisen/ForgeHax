package dev.fiki.forgehax.main.util.key;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.*;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Type;

import java.util.*;

import static dev.fiki.forgehax.main.Common.getLogger;

@Getter
public class KeyInput {
  private static final List<KeyInput> REGISTRY = Lists.newArrayList();
  private static final Map<Integer, KeyInput> CODE_TO_KEYINPUT = Maps.newHashMap();
  private static final Map<String, KeyInput> NAME_TO_KEYINPUT = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);

  // this is to force KeyInputs class to initialize
  private static final KeyInput INVALID = KeyInputs.UNBOUND;

  public static List<KeyInput> getRegistry() {
    return Collections.unmodifiableList(REGISTRY);
  }

  public static Collection<Integer> getRegisteredCodes() {
    return CODE_TO_KEYINPUT.keySet();
  }

  public static Collection<String> getRegisteredKeyNames() {
    return NAME_TO_KEYINPUT.keySet();
  }

  public static KeyInput getKeyInputByCode(int code) {
    return CODE_TO_KEYINPUT.getOrDefault(code, INVALID);
  }

  public static KeyInput getKeyInputByName(String key) {
    return NAME_TO_KEYINPUT.getOrDefault(key, INVALID);
  }

  private Type type;
  private final int code;
  private final String name;
  private final Set<String> aliases;

  @Builder(access = AccessLevel.MODULE)
  public KeyInput(@NonNull Type type, int code, @NonNull String name, @Singular Set<String> aliases) {
    this.type = type;
    this.code = code;
    this.name = name;
    this.aliases = ImmutableSet.copyOf(aliases);

    REGISTRY.add(this);
    CODE_TO_KEYINPUT.put(getCode(), this);
    for (String key : getKeyNames()) {
      if (NAME_TO_KEYINPUT.put(key, this) != null) {
        getLogger().warn("Duplicate key name \"{}\" replaced", key);
      }
    }
  }

  public boolean isMouseCode() {
    return Type.MOUSE.equals(type);
  }

  public boolean isKeyboardCode() {
    return Type.KEYSYM.equals(type);
  }

  public InputMappings.Input getInputMapping() {
    return isMouseCode() ? Type.MOUSE.getOrMakeInput(getCode())
        : Type.KEYSYM.getOrMakeInput(getCode());
  }

  public Collection<String> getKeyNames() {
    Set<String> names = Sets.newTreeSet(String.CASE_INSENSITIVE_ORDER);
    names.addAll(getAliases());
    names.add(getName());

    // remove the unnecessary extra string
    if (isMouseCode() && getName().startsWith("key.")) {
      names.add(getName().substring("key.".length()));
    } else if (isKeyboardCode() && getName().startsWith("key.keyboard.")) {
      names.add(getName().substring("key.keyboard.".length()));
    }

    return names;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    KeyInput keyInput = (KeyInput) o;
    return code == keyInput.code;
  }

  @Override
  public int hashCode() {
    return Objects.hash(code);
  }

  @Override
  public String toString() {
    return "KeyInput{" +
        "code=" + code +
        ", name='" + name + '\'' +
        '}';
  }
}

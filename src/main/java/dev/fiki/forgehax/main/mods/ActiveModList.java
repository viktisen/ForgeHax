package dev.fiki.forgehax.main.mods;

import dev.fiki.forgehax.main.Common;
import dev.fiki.forgehax.main.util.SimpleTimer;
import dev.fiki.forgehax.main.util.cmd.flag.EnumFlag;
import dev.fiki.forgehax.main.util.cmd.settings.BooleanSetting;
import dev.fiki.forgehax.main.util.cmd.settings.EnumSetting;
import dev.fiki.forgehax.main.util.cmd.settings.IntegerSetting;
import dev.fiki.forgehax.main.util.cmd.settings.LongSetting;
import dev.fiki.forgehax.main.util.color.Colors;
import dev.fiki.forgehax.main.util.draw.SurfaceHelper;
import dev.fiki.forgehax.main.util.math.AlignHelper;
import dev.fiki.forgehax.main.util.mod.AbstractMod;
import dev.fiki.forgehax.main.util.mod.Category;
import dev.fiki.forgehax.main.util.mod.HudMod;
import dev.fiki.forgehax.main.util.mod.loader.RegisterMod;
import dev.fiki.forgehax.main.mods.services.TickRateService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@RegisterMod
public class ActiveModList extends HudMod {

  private final BooleanSetting tps_meter = newBooleanSetting()
      .name("tps-meter")
      .description("Shows the server tps")
      .defaultTo(true)
      .build();

  private final BooleanSetting debug = newBooleanSetting()
      .name("debug")
      .description("Disables debug text on mods that have it")
      .defaultTo(false)
      .build();

  private final LongSetting timeoutDisplay = newLongSetting()
      .name("timeout-display")
      .description("Time required to elapse until lag in ms is shown")
      .defaultTo(1000L)
      .min(1L)
      .build();

  private final BooleanSetting showLag = newBooleanSetting()
      .name("show-lag")
      .description("Shows lag time since last tick")
      .defaultTo(true)
      .build();

  private final EnumSetting<SortMode> sortMode = newEnumSetting(SortMode.class)
      .name("sorting")
      .description("Sorting mode")
      .defaultTo(SortMode.ALPHABETICAL)
      .build();

  @Override
  protected AlignHelper.Align getDefaultAlignment() {
    return AlignHelper.Align.TOPLEFT;
  }

  @Override
  protected int getDefaultOffsetX() {
    return 1;
  }

  @Override
  protected int getDefaultOffsetY() {
    return 1;
  }

  @Override
  protected double getDefaultScale() {
    return 1d;
  }

  public ActiveModList() {
    super(Category.RENDER, "ActiveMods", true, "Shows list of all active mods");
    addFlag(EnumFlag.HIDDEN);
  }

  private String generateTickRateText() {
    String text = "Tick-rate: ";
    TickRateService monitor = TickRateService.getInstance();
    if(!monitor.isEmpty()) {
      text += String.format("%1.2f", monitor.getRealtimeTickrate());

      if(showLag.getValue()) {
        text += " : ";
        TickRateService.TickrateTimer current = monitor.getCurrentTimer();
        if (current != null
            && current.getTimeElapsed() > timeoutDisplay.getValue()) {
          text += String.format("%01.1fs", (float) (current.getTimeElapsed() - 1000L) / 1000.f);
        } else {
          text += "0.0s";
        }
      }
    } else {
      text += "<unavailable>";
    }

    return text;
  }

  @SubscribeEvent
  public void onRenderScreen(RenderGameOverlayEvent.Text event) {
    int align = alignment.getValue().ordinal();

    List<String> text = new ArrayList<>();

    if (tps_meter.getValue()) {
      text.add(generateTickRateText());
    }

    if (Common.getDisplayScreen() instanceof ChatScreen || Common.MC.gameSettings.showDebugInfo) {
      long enabledMods = Common.getModManager()
          .getMods()
          .stream()
          .filter(AbstractMod::isEnabled)
          .filter(mod -> !mod.isHidden())
          .count();
      text.add(enabledMods + " mods enabled");
    } else {
      Common.getModManager()
          .getMods()
          .stream()
          .filter(AbstractMod::isEnabled)
          .filter(mod -> !mod.isHidden())
          .map(mod -> debug.getValue() ? mod.getDebugDisplayText() : mod.getDisplayText())
          .sorted(sortMode.getValue().getComparator())
          .forEach(name -> text.add(AlignHelper.getFlowDirX2(align) == 1 ? ">" + name : name + "<"));
    }

    SurfaceHelper.drawTextAlign(text, getPosX(0), getPosY(0),
        Colors.WHITE.toBuffer(), scale.getValue(), true, align);
  }

  private enum SortMode {
    ALPHABETICAL((o1, o2) -> 0), // mod list is already sorted alphabetically
    LENGTH(Comparator.<String>comparingInt(SurfaceHelper::getTextWidth).reversed());

    private final Comparator<String> comparator;

    public Comparator<String> getComparator() {
      return this.comparator;
    }

    SortMode(Comparator<String> comparatorIn) {
      this.comparator = comparatorIn;
    }
  }
}

import java.lang.reflect.Method;
import net.minecraft.world.entity.player.Player;

public class FindPlayerMethods {
    public static void main(String[] args) throws Exception {
        for (Method m : Player.class.getMethods()) {
            if (m.getName().toLowerCase().contains("message") || m.getName().toLowerCase().contains("display")) {
                System.out.println(m.getName() + " " + java.util.Arrays.toString(m.getParameterTypes()));
            }
        }
    }
}

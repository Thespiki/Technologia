package dev.technologia.machine;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class EnergyBufferTest {
    @Test void simulationNeverMutatesAndTransfersConserveEnergy() {
        EnergyBuffer source = new EnergyBuffer(1_000_000), target = new EnergyBuffer(50_000);
        source.restore(900_000);
        Random random = new Random(1122);
        for (int i = 0; i < 20_000; i++) {
            int total = source.stored() + target.stored();
            int offered = source.extract(random.nextInt(1000), true);
            int accepted = target.receive(offered, true);
            assertEquals(total, source.stored() + target.stored());
            assertEquals(accepted, source.extract(accepted, false));
            assertEquals(accepted, target.receive(accepted, false));
            assertEquals(total, source.stored() + target.stored());
            int back = target.extract(random.nextInt(800), false);
            source.receive(back, false);
            assertEquals(total, source.stored() + target.stored());
        }
    }
    @Test void malformedAndOverflowAmountsStayBounded() {
        EnergyBuffer buffer = new EnergyBuffer(1_000_000);
        assertEquals(0, buffer.receive(-1, false));
        assertEquals(1_000_000, buffer.receive(Integer.MAX_VALUE, false));
        assertEquals(0, buffer.receive(Integer.MAX_VALUE, false));
        assertEquals(0, buffer.extract(Integer.MIN_VALUE, false));
        assertEquals(1_000_000, buffer.extract(Integer.MAX_VALUE, false));
        buffer.restore(Integer.MIN_VALUE); assertEquals(0, buffer.stored());
        buffer.restore(Integer.MAX_VALUE); assertEquals(buffer.capacity(), buffer.stored());
        assertThrows(IllegalArgumentException.class, () -> new EnergyBuffer(0));
    }
}

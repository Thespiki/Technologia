package dev.technologia.machine;

/** One energy unit is one FE (Forge/NeoForge) or E (Fabric Team Reborn Energy). */
public final class EnergyBuffer {
    private int capacity;
    private int stored;
    public EnergyBuffer(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity");
        this.capacity = capacity;
    }
    public int receive(int amount, boolean simulate) {
        int accepted = Math.min(Math.max(0, amount), capacity - stored);
        if (!simulate) stored += accepted;
        return accepted;
    }
    public int extract(int amount, boolean simulate) {
        int extracted = Math.min(Math.max(0, amount), stored);
        if (!simulate) stored -= extracted;
        return extracted;
    }
    public void restore(int amount) { stored = Math.clamp(amount, 0, capacity); }
    /** A tier change resizes the buffer; stored energy above a smaller capacity is clipped. */
    public void resize(int newCapacity) {
        if (newCapacity < 1) throw new IllegalArgumentException("capacity");
        capacity = newCapacity;
        stored = Math.min(stored, capacity);
    }
    public int stored() { return stored; }
    public int capacity() { return capacity; }
}

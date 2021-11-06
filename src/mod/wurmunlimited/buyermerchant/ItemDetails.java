package mod.wurmunlimited.buyermerchant;

public class ItemDetails {
    public final int weight;
    public final float minQL;
    public final int price;
    public final int remainingToPurchase;
    public final int minimumPurchase;
    public final boolean acceptsDamaged;

    public ItemDetails(int weight, float minQL, int price, int remainingToPurchase, int minimumPurchase, boolean acceptsDamaged) {
        this.weight = weight;
        this.minQL = minQL;
        this.price = price;
        this.remainingToPurchase = remainingToPurchase;
        this.minimumPurchase = minimumPurchase;
        this.acceptsDamaged = acceptsDamaged;
    }
}

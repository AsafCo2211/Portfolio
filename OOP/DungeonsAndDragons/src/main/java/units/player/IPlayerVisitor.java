package units.player;

public interface IPlayerVisitor {
    void visit(Warrior w);
    void visit(Mage m);
    void visit(Rogue r);
    void visit(Hunter h);
}
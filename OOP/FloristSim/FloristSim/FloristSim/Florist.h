#ifndef FLORIST_H
#define FLORIST_H

#include <string>
#include <vector>

class Wholesaler;
class FlowerArranger;
class DeliveryPerson;
class Person;
class FlowersBouquet;

class Florist {
private:
    std::string name;
    Wholesaler* wholesaler;
    FlowerArranger* arranger;  
    DeliveryPerson* deliveryPerson;

public:
    Florist(std::string name, Wholesaler* wholesaler, FlowerArranger* arranger, DeliveryPerson* deliveryPerson);
    void acceptOrder(Person* customer, std::vector<std::string>& flowers);
    std::string getName();
};

#endif
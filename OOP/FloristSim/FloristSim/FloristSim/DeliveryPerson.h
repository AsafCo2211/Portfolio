#ifndef DELIVERY_PERSON_H
#define DELIVERY_PERSON_H

#include <string>
class Person;
class FlowersBouquet;

class DeliveryPerson {
private:
    std::string name;

public:
    DeliveryPerson(std::string name);
    void deliver(Person* recipient, FlowersBouquet* bouquet);
    std::string getName();
};

#endif 
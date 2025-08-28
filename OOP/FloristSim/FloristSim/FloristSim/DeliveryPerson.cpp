#include <iostream>
#include "DeliveryPerson.h"
#include "FlowersBouquet.h"
#include "Person.h"

DeliveryPerson::DeliveryPerson(std::string name) : name(name) {}

void DeliveryPerson::deliver(Person* recipient, FlowersBouquet* bouquet) {
    std::cout << "Delivery Person " << name << " delivers flowers " << recipient->getName() << ".\n";
    recipient->acceptFlowers(bouquet);
}

std::string DeliveryPerson::getName() {
    return name;
}

#include <string>
#include <vector>
#include <iostream>

#include "Person.h"
#include "Florist.h"
#include "Wholesaler.h"
#include "FlowerArranger.h"
#include "DeliveryPerson.h"

Florist::Florist(std::string name, Wholesaler* wholesaler, FlowerArranger* arranger, DeliveryPerson* deliveryPerson)
    : name(name), wholesaler(wholesaler), arranger(arranger), deliveryPerson(deliveryPerson) {}

void Florist::acceptOrder(Person* recipient, std::vector<std::string>& flowerTypes) {
    std::cout << "Florist " << name << " forwards request to Wholesaler " << wholesaler->getName() << ".\n";

    FlowersBouquet* bouquet = wholesaler->acceptOrder(flowerTypes);

    std::cout << "Wholesaler " << wholesaler->getName() << " returns flowers to Florist " << name << ".\n";
    std::cout << "Florist " << name << " request flowers arrangement from Flower Arranger " << arranger->getName() << ".\n";

    arranger->arrangeFlowers(bouquet);

    std::cout << "Flower Arranger " << arranger->getName() << " returns arranged flowers to Florist " << name << ".\n";
    std::cout << "Florist " << name << " forwards flowers to Delivery Person " << deliveryPerson->getName() << ".\n";

    deliveryPerson->deliver(recipient, bouquet);
}

std::string Florist::getName() {
    return name;
}

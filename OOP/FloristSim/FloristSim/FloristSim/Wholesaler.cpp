#include "Wholesaler.h"
#include "Grower.h"
#include "FlowersBouquet.h"
#include <iostream>
#include <vector>
#include <string>

Wholesaler::Wholesaler(std::string name, Grower* grower) : name(name), grower(grower) {}

FlowersBouquet* Wholesaler::acceptOrder(std::vector<std::string>& flowerTypes) {
    std::cout << "Wholesaler " << name << " forwards the request to Grower " << grower->getName() << ".\n";

    FlowersBouquet* bouquet = grower->prepareOrder(flowerTypes);

    std::cout << "Grower " << grower->getName() << " returns flowers to Wholesaler " << name << ".\n";

    return bouquet;
}

std::string Wholesaler::getName() {
    return name;
}
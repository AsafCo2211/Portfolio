#include <vector>
#include <string>
#include <iostream>

#include "Grower.h"  
#include "Gardener.h"
#include "FlowersBouquet.h"

Grower::Grower(std::string name, Gardener* gardener) : name(name), gardener(gardener) {}

FlowersBouquet* Grower::prepareOrder(std::vector<std::string>& flowerTypes) {
    std::cout << "Grower " << name << " forwards the request to Gardener " << gardener->getName() << ".\n";

    FlowersBouquet* bouquet = gardener->prepareBouquet(flowerTypes);

    std::cout << "Gardener " << gardener->getName() << " returns flowers to Grower " << name << ".\n";

    return bouquet;
}

std::string Grower::getName() {
    return name;
}

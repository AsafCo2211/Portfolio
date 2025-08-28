#include <vector>
#include <string>
#include <iostream>

#include "Gardener.h"
#include "FlowersBouquet.h"

Gardener::Gardener(std::string name) : name(name) {}

FlowersBouquet* Gardener::prepareBouquet(std::vector<std::string>& flowerTypes) {
    std::cout << "Gardener " << name << " prepares flowers.\n";
    return new FlowersBouquet(flowerTypes);
}

std::string Gardener::getName() {
    return name;
}
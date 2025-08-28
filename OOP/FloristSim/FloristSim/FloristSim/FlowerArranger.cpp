#include <iostream>
#include "FlowerArranger.h"
#include "FlowersBouquet.h"

FlowerArranger::FlowerArranger(std::string name) : name(name) {}

void FlowerArranger::arrangeFlowers(FlowersBouquet* bouquet) {
    std::cout << "Flower Arranger " << name << " arranges flowers.\n";
    bouquet->arrange();
}

std::string FlowerArranger::getName() {
    return name;
}
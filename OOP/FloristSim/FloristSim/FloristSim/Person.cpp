#include <string>
#include <iostream>
#include <vector>

#include "Person.h"
#include "Florist.h"
#include "FlowersBouquet.h"

Person::Person(std::string name) : name(name) {}

void Person::orderFlowers(Florist* florist, Person* recipient, std::vector<std::string>& flowerTypes) {
    std::cout << name << " orders flowers to " << recipient->getName()
              << " from Florist " << florist->getName() << ": ";

    for (int i = 0; i < flowerTypes.size(); ++i) {
        std::cout << flowerTypes[i];
        if (i + 1 < flowerTypes.size())
            std::cout << ", ";
        else
            std::cout << ".\n";
    }

    florist->acceptOrder(recipient, flowerTypes);
}

void Person::acceptFlowers(FlowersBouquet* bouquet) {
    std::cout << name << " accepts the flowers: ";

    for (int i = 0; i < bouquet->getBouquet().size(); ++i) {
        std::cout << bouquet->getBouquet()[i];
        if (i + 1 < bouquet->getBouquet().size())
            std::cout << ", ";
        else
            std::cout << ".";
    }
}

std::string Person::getName() {
    return name;
}

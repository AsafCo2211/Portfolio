#ifndef WHOLESALER_H
#define WHOLESALER_H

#include <string>
#include <vector>
class Grower;
class FlowersBouquet;

class Wholesaler {
private:
    std::string name;
    Grower* grower;

public:
    Wholesaler(std::string name, Grower* grower);
    FlowersBouquet* acceptOrder(std::vector<std::string>& flowers);
    std::string getName();
};

#endif
# Forex trading neural network trained by evolution

This is an old project from 2017.<br/>

## Idea
Since I'm not a trader by profession I really don't know much trading
using technical analysis and supervised machine learning requires 
an expert to label the data. <br/>
Given these limitations, one way to "train" an artificial neural network to trade
that I could come up with was to evolve it using genetic algorithms.<br/>

The idea was to gather historic data on forex prices (USD/EUR) and
calculate some technical analysis indicators that traders usually use such 
as bollinger bands etc.<br/>
Next a population (e.g. 500 individuals) of NNs are generated with random weights 
on the connections. The inputs to the NNs are prices and indicator values we created for each 
timestamp. The outputs of the network are buy/sell/hold/pass signals. Each of the NNs are
be evaluated by simulating the trading on historic data.<br>
The fitness function is the profit made by NN by the end of the simulation of trading.
The top performing NNs are then be subjected to various genetic algorithms actions (selection, 
crossover, mutation, etc.) and the bottom ones are removed from the population pool. Finally,
the next generation of NNs is prepared to start the journey their ancestors did before them.<br/>

In theory after many generation the NNs would evolve to the point where all of them would
start making profit.
Unfortunately it seems the search space was too big to achieve any success with this approach.
# Forex trading neural network trained by evolution

This is an old project from 2017.<br/>

## Idea
Since I'm not a professional trader, I don't know much about trading
using technical analysis, and supervised machine learning requires
an expert to label the data. <br/>
Given these limitations, one way to "train" an artificial neural network to trade
that I could come up with was to evolve it using genetic algorithms.<br/>

The idea was to gather historical data on forex prices (USD/EUR) and
calculate some technical analysis indicators that traders usually use, such
as Bollinger bands etc.<br/>
Next, a population (500 or so individuals) of NNs is generated with random weights
on the connections. The inputs to the NNs are prices and indicator values we created for each
timestamp. The neural network's outputs are buy/sell/hold/pass signals. Each of the NNs is evaluated by simulating the trading on historical data.<br>
The fitness function is the profit made by NN by the end of the trading simulation.
The top-performing NNs are then be subjected to various genetic algorithms actions (selection,
crossover, mutation, etc.), and the bottom ones are removed from the population pool. Finally,
the next generation of NNs is prepared to start the journey their ancestors did before them.<br/>

In theory, after many generation, the NNs would evolve to the point where all of them would
start making a stady profit.
Unfortunately, it seems the search space was too big to achieve any success with this approach.

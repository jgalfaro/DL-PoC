#Supplementary Material to: Blueprint for Next Generation
#Cyber-Physical Resilience using Defense Quantum Machine Learning
#
#Michel Barbeau, Carleton University, School of Computer Science, Canada.
#
#Joaquin Garcia-Alfaro, Institut Polytechnique de Paris, France.
#
# Version: April 21, 2020
#
# We reused code from:
#
# Basic tutorial: qubit rotation (cf. https://pennylane.ai/qml/demos/tutorial_qubit_rotation.html)
#
# Note: This example requires python3 and version 0.8.0 of Pennylane,
# which is not the defaul latest version. To force installation of
# that version, in a Linux shell enter:
#
# python3 -m pip install pennylane==0.8.0
#
# or
#
# pip3 install pennylane==0.8.0
#
# The example also requires the StrawberryFields plugin. You can
# install the required plugin with the following command:
#
# python3 -m pip install pennylane-sf
#
# or
#
# pip3 install pennylane-sf
#
#
import pennylane as qml
from pennylane import numpy as np
import matplotlib.pyplot as plt

dev1 = qml.device("default.qubit", wires=1, shots=1)

@qml.qnode(dev1)
def W(theta):
    qml.RX(theta[0], wires=0)
    qml.RY(theta[1], wires=0)
    # return probabilities of computational basis states
    return qml.probs(wires=[0])

def square_loss(X, Y):
    loss = 0
    for x, y in zip(X, Y):
        loss = loss + (x - y) ** 2
    return loss / len(X)

def cost(theta, p):
    #  p is prob. of ket zero, 1-p is prob of ket 1
    return square_loss(W(theta), [p, 1-p])

# initialise the optimizer
opt = qml.GradientDescentOptimizer(stepsize=0.4)
# set the number of steps
steps = 100
# set the initial theta value of variational circuit state
theta = np.random.randn(2)
print("Initial probs. of basis states: {}".format(W(theta)))

def update(theta, p):
    # p = probability
    for i in range(steps):
        # update the circuit parameters
        theta = opt.step(lambda v: cost(v, p), theta)
        #if (i + 1) % 10 == 0:
            #print("Cost @ step {:5d}: {: .7f}".format(i, cost(theta,p)))
    print("Probs. of basis states: {}".format(W(theta)))
    return theta


# init theta to random values
theta = np.random.randn(2)
# number of iterations
epochs = 100
# measurement results (probs. of ket zero and ket one)
M = np.zeros(epochs)
# environment probability
p = 0.5
# rewards
R = [4, 2]
# Q-value (total reward associated with actions)
Q = [0, 0]
# learning rate
alpha = 0.5
# main loop
for i in range(epochs):
    # random action
    a = np.random.randint(2, size=1)[0]
    # determine reward
    num = np.random.random(1)[0]
    r = 0
    if num > p:
        r = R[a]
    # Bellman equation
    Q[a] = (1-alpha)*Q[a] + alpha*(r + Q[a])
    print("action: ", a, " reward: ", r, " Q: ", Q)
    if (Q[0]+Q[1]) > 0:
        a=0
        theta = update(theta, Q[0]/(Q[0]+Q[1]))
    M[i] = W(theta)[0]



plt.plot(M, 'bs', label='take loop')
plt.plot(1-M, 'g^', label='take bypass')
plt.xlabel('Epoch')
plt.ylabel('Probability')
plt.grid(True)
plt.legend()
plt.show()

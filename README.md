# RxExamples
A compilation of RxExamples

## RetryWhen
The RetryWhen example illustrates how to setup a subscription to try 3 times to received a successful response before causing the subscription to receive an error response.  Each subsequent request will wait longer, in order to give the API a chance to recover or data availability to improve.

The subscription will not be retried if the service sends a status of 401, Authentication Required.

Please try out the example, it is a simple app that searches for a GitHub user and if found, displays the users Avatar

### Testing the Retries
The easiest way that I found to manually test that the retries is to turn on Airplane Mode.  After starting a query, then turn Airplane Mode off.  Watch Android Monitor and see your request getting retried!

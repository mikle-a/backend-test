# Backend Test

Server provides RESTful API for money transfers between accounts.
Vertx is used for handling http and internal storage is backed with H2.

## Build and run

1) Build the appliction:
`gradle clean build copyRuntimeLibs`

2) Run with
`java -cp "build\libs\*" com.revolut.backend.BackendServer` or `run.bat`/`run.sh`

By default, server runs on port 8080. Use **-p** option to specify another one.

## Usage

### Create user

URL: `/user`

Method: `POST `

Required parameters:
  * userName

Success response example:

    Code: 200
    Content-type: application/json
    Body: {"userId" : 1}  

Error response example:

    Code: 400
    Content-type: application/json
    Body: {"error":"Error during validation of request. Parameter \"userName\" inside query not found"}

### Create account

URL: `/account`

Method: `POST`

Required headers:
  * userId

Optional parameters:
  * balance - initial amount, 0 by default

Success response example:

    Code: 200
    Content-type: application/json
    Body: {"accountId" : 1}  

Error response example:

    Code: 404
    Content-type: application/json
    Body: {"error":"User not found"}

### Get account

URL: `/account/:id`

Method: `GET`

Required headers:
  * userId - account owner

Success response example:

    Code: 200
    Content-type: application/json
    Body: {"accountId" : 1, "balance" : "99.90"}  

Error response example:

    Code: 404
    Content-type: application/json
    Body: {"error":"Account not found"}
    
### Make transfer

URL: `/account/:id/transfer`

Method: `PATCH`

Required headers:
  * userId - account owner
  
Required parameters:
  * dstAccountId - account recipient
  * amount

Success response example:

    Code: 200
    Content-type: application/json
    Body: {"transferId" : 1}  

Error response example:

    Code: 409
    Content-type: application/json
    Body: {"error":"Insufficient funds"}
    
### Get transfer

URL: `/transfer/:id`

Method: `GET`

Required headers:
  * userId - transfer owner

Success response example:

    Code: 200
    Content-type: application/json
    Body: {
          	"transferId": 1,
          	"srcAccountId": 1,
          	"dstAccountId": 2,
          	"userId": 1,
          	"timestamp": 1549052019119,
          	"amount": "55.73",
          	"srcAccountBalanceBefore": "100",
          	"srcAccountBalanceAfter": "44.27",
          	"dstAccountBalanceBefore": "0",
          	"dstAccountBalanceAfter": "55.73"
          } 

Error response example:

    Code: 409
    Content-type: application/json
    Body: {"error":"Transfer not found"}
    
## Testing
API demonstration with tests could be found in `com.revolut.backend.component.test.BackendServerComponentTest`
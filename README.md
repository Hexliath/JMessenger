# JMessenger Server

> Server application for JMessenger - ISEN 2019

#### Doc version : 1.0.1 - 15/03/2019 - XML export

---

## Compile and run

You will need : 

> Java Open JDK 8
> 
> Apache maven 3

1. Go to src/main/resources/app_config.json

Enter your posgres database configuration, for example :

```json
    "sql_host": "localhost",
    "sql_port": "5432",
    "sql_database": "postgres",
    "sql_schema": "messenger",
    "sql_user": "postgres",
    "sql_password": "verystrongpasswrod",
```

> Database user must have enaugh priviledge to `alter user role set search_patch` 
> 
> Database user must have all priviledges on messenger schema
> 
> Schema must ne empty (no tables and sequences) on the first launch

2. Compile jar with maven

```bash
mvn package
```

3. Run app

```bash
java -jar messenger-server-1.0.0-jar-with-dependencies.jar
```

### Dockerise

You can dockerise the app, dockerfile example : 

```docker
FROM java:8
WORKDIR /
ADD jmessenger.jar jmessenger.jar
EXPOSE 6100
EXPOSE 6101
CMD ["java", "-jar", "jmessenger.jar"]
```

```docker
docker build -t jmessenger .
```

```docker
docker run -i -t -p 6100:6100 -p 6101:6101 jmessenger
```

#### Temporary database

You may want to use a temporary postgres database to test the app. Remember to create a blank schema before launching server app.

```docker
docker run --rm --name postgres -e POSTGRES_PASSWORD=verystrongpasswrod -d -p 5432:5432 postgres
```

## API

A REST complient api. 

Default root path: /api/v1

Timestamps are expressed in milliseconds

### Health check

**ANY** /ping

```json
Request: GET /ping
```

```json
Result: 200 OK (RAW TEXT BODY)
pong
```

---

### Authorization

Tokens are valid during several hours. The lifespan of token is extended at every api call. A logout destroys the token. Every api call requires Auth header (unless otherwise stated) :

```json
Authorization: token
```

---

#### Create an account (no Authorization required)

**POST** /auth/register

> Requires a Login body
> 
> Returns new user
> 
> If no displayName provided, will be set same as login

```json
Request: POST /auth/register
{
    "login": "user123",
    "password": "password123",
    "display_name": "TheUser123"
}
```

```json
Result: 200 OK
{
    "id": "b6a409b9-3387-4a64-9dc0-82cc932c177d",
    "login": "user123",
    "display_name": "ThUser123"
}
```

```java
ERROR CODES
- LOGOUT_REQUIRED
- ALREADY_EXIST
```

---

#### Login and get token (no Authorization required)

**POST** /auth/login

> Requires a Login body.   
> Returns a LoginResult

```json
Request: POST /auth/login
{
    "login": "user123",
    "password": "password123"
}
```

```json
Result: 200 OK
{
    "code": "SUCCESS",
    "success": true,
    "attempt": 1,
    "locked": false,
    "token": "tokentokentokentokentokentokentokentokentokentokentokentokentoke",
    "message": null,
    "id": "123e4567-e89b-12d3-a456-556642440000"
}
```

```json
Result: 200 OK
{
    "code": "TOO_MANY_ATTEMPTS",
    "success": false,
    "attempt": 3,
    "locked": true,
    "token": "",
    "message": "You are temporary blocked for to many false login attempts. Try again later",
    "id": null
}
```

```java
ERROR CODES ( codes will be send in body )
- SUCCESS
- TOO_MANY_CONNECTIONS
- WRONG_PASSWORD
- TOO_MANY_ATTEMPTS
- TRY_AGAIN_LATER
```

---

#### Logout

**POST** /auth/logout

> Requires Authorization token
> 
> Destroys current Authorization token

```json
Request: POST /auth/logout
```

```json
Result: 200 OK
```

```java
ERROR CODES
( no error codes expected )
```

---

#### Logout from all sessions

**POST** /auth/logout/all

> Requires Authorization token
> 
> Destorys all Authorization tokens attached to source user

```json
Request: POST /auth/logout/all
```

```json
Result: 200 OK
```

```java
ERROR CODES
( no error codes expected )
```

### Users

---

#### Get self information

**GET** /users/me

> Returns User.

```json
Request: GET /users/me
```

```json
Result: 200 OK
{
    "id": "8c0cc8ac-03f7-472d-b8b7-bbbc893eb08c",
    "login": "user123",
    "display_name": "TheUser123"
}
```

```java
ERROR CODES
( no error codes expected )
```

---

#### Change display name

**PATCH** /users/me

> Changes the display name

```json
Request: PATCH /users/me
{
    "display_name": "ProUser2000"
}
```

```json
Result: 200 OK
{
    "id": "8c0cc8ac-03f7-472d-b8b7-bbbc893eb08c",
    "login": "user123",
    "display_name": "ProUser2000"
}
```

```json
ERROR CODES
- NAME_UNAVAILABLE
- WRONG_LENGTH
- ELEMENT_NOT_FOUND
```

---

#### Get user information

**GET** /users/{user_id}

> Returns User

```json
Request: GET /users/2083c77e-d367-43b5-84f9-14788180253d
```

```json
Result: 200 OK
{
    "id": "2083c77e-d367-43b5-84f9-14788180253d",
    "login": "megaman",
    "display_name": "CoolBro777"
}
```

```java
ERROR CODES
- USER_NOT_FOUND
```

---

#### Get users

**GET** /users/search?name={login/dsiplay_name}&online=true

> Returns List of User
> 
> If no **name** specified, then searches for all users
> 
> If **name** specified, then only players with coresponding login/name will be searched
> 
> If **online=true** same search will be done, but only online users will be shown
> 
> online=false has no effect, offline and online users will be shown
> 
> Search is case unsensitive, it uses LIKE %value%, accepts from 3 to 30 characters 
> 
> Resulting Users contain **online** field witch indicates whether user is connected or not to push service

```json
Request: GET /users/search?name=the&online=true
```

```json
Result: 200 OK
[
    {
        "id": "8c0cc8ac-03f7-472d-b8b7-bbbc893eb08c",
        "login": "user123",
        "display_name": "TheUser123",
        "online": true
    }
]
```

```java
ERROR CODES
( no error codes expected )
```

### Messages

---

#### Send message

**POST** /messages

> Requires OWNER / ADMIN / MEMBER role.  
> Requires a Message body with two content and channel id
> Returns the created message

```json
Request: POST /messages
{
    "content": "Hello world!",
    "channel_id": "7d54138f-b49c-456a-9788-4304f7ed53a0"
}
```

```json
Result: 200 OK
{
    "id": "8c0cc8ac-03f7-472d-b8b7-bbbc893eb08c",
    "content": "Hello World!",
    "author": {
        "id": "b6a409b9-3387-4a64-9dc0-82cc932c177d",
        "login": "user123",
        "display_name": "user123"
        },
    "channel_id": "123e4567-e89b-12d3-a456-556642440000",
    "creation_time": 1234567891234
}
```

```java
ERROR CODES
- NOT_ENOUGH_PRIVILEGES
```

### Channels

---

#### List my channels

**GET** /channels 

> Returns a Channel List of channels where source user is OWNER, ADMIN or MEMBER
> 
> Includes addintionnal **joined** attribute indicating whether user is member or not of this channel
> 
> Also includes **PUBLIC** channels that source user is not yet a member

```json
Request: GET /channels
```

```json
Result: 200 OK
[
    {
        "id": "378c8c32-3f85-11e9-b210-d663bd873d93",
        "name": "CoolBro777",
        "type": "PRIVATE",
        "members": [
            {
                "id": "2083c77e-d367-43b5-84f9-14788180253d",
                "login": "megaman",
                "display_name": "CoolBro777"
            }
        ],
        "owner": {
            "id": "b6a409b9-3387-4a64-9dc0-82cc932c177d",
            "login": "user123",
            "display_name": "user123"
        },
        "join_time": 1552139884831,
        "joined": true
    },
    {
        "id": "378c95f6-3f85-11e9-b210-d663bd873d93",
        "name": "Pro Gamers",
        "type": "PUBLIC",
        "members": [
            {
                "id": "b6a409b9-3387-4a64-9dc0-82cc932c177d",
                "login": "user123",
                "display_name": "user123"
            }
        ],
        "owner": {
            "id": "9a269205-7c36-4b86-abff-256a49be4e76",
            "login": "masterrace",
            "display_name": "PC Master Race"
        },
        "join_time": 1552139884831,
        "joined": false
    }
]
```

---

#### Create a channel

**POST** /channels

> Requires a Channel body.    
> Do not include owner (source) user in query. He will be automaticy added and defined as OWNER
> Returns the created Channel

Query examples

```json
POST /channels
{
    "name": "My new channel"
}
>>>> create a PUBLIC channel
```

```json
POST /channels
{
    "name": "My new channel",
    "type": "PUBLIC",
    "members": [
        {
            "user": {
                "id": "123e4567-e89b-12d3-a456-556642440000"
            },
            "role": "ADMIN"
        }
    ]
}
>>>> create a PUBLIC channel
```

```json
POST /channels
{
    "name": "My new private chat",
    "members": [
        {
            "user": {
                "id": "123e4567-e89b-12d3-a456-556642440000"
            }
        }
    ]
}
>>>> create a PRIVATE channel
```

```json
POST /channels
{
    "name": "My new group",
    "type": "GROUP",
    "members": [
        {
            "user": {
                "id": "123e4567-e89b-12d3-a456-556642440000"
            }
        }
    ]
}
>>>> create a GROUP channel
```

```json
POST /channels
{
    "name": "My new group",
    "members": [
        {
            "user": {
                "id": "123e4567-e89b-12d3-a456-556642440000"
            }
        },
        {
            "user": {
                "id": "4b36869e-d44a-4359-8c2c-56dd3b30664c"
            }
        }
    ]
}
>>>> create a GROUP channel
```

```json
Result: 200 OK
{
    "id": "e420a008-1084-43fd-a72c-c9fa33a4b39d",
    "name": "Free 4 All",
    "type": "PUBLIC",
    "members": [],
    "owner": {
        "id": "92cc6909-bdf4-4a90-8f0c-8b07fa9d3282",
        "login": "megaman",
        "display_name": "__MEGAMAN__"
    },
    "join_time": 1552293021464
}
```

```java
ERROR CODES
- UNAVAILABLE_CHANNEL_NAME
```

---

#### Edit a channel

**PATCH** /channels/*{channel_id}*

> Requires OWNER / ADMIN role to modify name
> 
> Requires OWENR role to modify owner 
> 
> Only name and owner are editable
> 
> Returns the updated channel

```json
Request: PATCH /channels/378c8c32-3f85-11e9-b210-d663bd873d93
{ 
    "name": "The new better name",
    "owner": {
        "id": "b6a409b9-3387-4a64-9dc0-82cc932c177d"
    }
}
```

```json
Result: 200 OK 
{
    "id": "378c8c32-3f85-11e9-b210-d663bd873d93",
    "name": "The new better name",
    "type": "PUBLIC",
    "members": [
        {
            "user": {
                "id": "b6a409b9-3387-4a64-9dc0-82cc932c177d",
                "login": "user123",
                "display_name": "TheUser123"
            },
            "role": "MEMBER"
        }
    ],
    "owner": { 
        "id": "2083c77e-d367-43b5-84f9-14788180253d",
        "login": "megaman",
        "display_name": "CoolBro777"
    },
    "join_time": 1552139884831
}
```

```java
ERROR CODES
- NOT_ENOUGH_PRIVILEGES
- ELEMENT_NOT_FOUND
```

## Roles

#### Set channel users role

**PATCH** /roles/*{channel_id}*

> Requires source have OWNER role in channel.
> 
> Requires ChannelUser
> 
> Returns the new ChannelUser elementEn 

Query example :

```json
Request: PATCH /roles/123e4567-e89b-12d3-a456-556642440000
{
    "user": {
        "id": "123e4567-e89b-12d3-a456-556642440000"
    },
    "role": "ADMIN"
}
```

```json
Result: 200 OK
{
    "user": {
        "id": "123e4567-e89b-12d3-a456-556642440000",
        "login": "user123",
        "display_name": "TheUser123"
    },
    "role": "ADMIN"
}
```

```java
ERROR CODES
- PARAMETER_REQUIRED
- PRIVILEGE_CONFLICT
- ELEMENT_NOT_FOUND
- NOT_ENOUGH_PRIVILEGES
- USER_NOT_FOUND
- NEED_TO_JOIN_CHANNEL
```

## User allocator

#### Add a user to GROUP channel

**POST** /allocator/add-to/*{channel_id}*

> Requires source to have OWNER / ADMIN role.
> 
> Requires a User body with target uuid at least.
> 
> Channel must be of type GROUP
> 
> Returns ChannelUser

Example query :

```json
Reqiest: POST /allocator/add-to/123e4567-e89b-12d3-a456-556642440000
{
    "id": "4b36869e-d44a-4359-8c2c-56dd3b30664c"
}
```

```json
Result: 200 OK
{
    "user": {
        "id": "4b36869e-d44a-4359-8c2c-56dd3b30664c",
        "login": "anonymous",
        "display_name": "anonymous"
    },
    "role": "MEMBER"
}
```

```java
ERROR CODES
- NOT_ENOUGH_PRIVILEGES
- ELEMENT_NOT_FOUND
- WRONG_CHANNEL_TYPE
- USER_NOT_FOUND
- ALREADY_EXIST
```

---

#### Remove user from GROUP/PUBLIC channel

**POST** /allocator/kick-from/*{channel_id}*

> Source must have OWNER / ADMIN role in channel.
> 
> Requires a User body with uuid at least
> 
> Channel must be of type GROUP or PUBLIC

Example query :

```json
Request: POST /allocator/kick-from/123e4567-e89b-12d3-a456-556642440000
{ 
    "id": "4b36869e-d44a-4359-8c2c-56dd3b30664c"
}
```

```json
Result: 200 OK
```

```java
ERROR CODES
- NOT_ENOUGH_PRIVILEGES
- ELEMENT_NOT_FOUND
- WRONG_CHANNEL_TYPE
- USER_NOT_FOUND
- NOT_A_MEMBER
```

---

#### Get user info specific to channel

**POST** /allocator/info-from/*{channel_id}*

> Source must have OWNER / ADMIN / MEMBER role in  channel.
> Returns ChannelUser

Example query :

```json
Request: POST /allocator/info-from/123e4567-e89b-12d3-a456-556642440000
{ 
    "id": "4b36869e-d44a-4359-8c2c-56dd3b30664c"
}
```

```json
Result: 200 OK
{
    "user": {
        "id": "4b36869e-d44a-4359-8c2c-56dd3b30664c",
        "login": "anonymous",
        "display_name": "anonymous"
    },
    "role": "MEMBER"
}
```

```java
ERROR CODES
- NOT_ENOUGH_PRIVILEGES
- ELEMENT_NOT_FOUND
- USER_NOT_FOUND
- NOT_A_MEMBER
```

---

#### Get channel users

**GET** /allocator/*{channel_id}*/users

> Requires OWNER / ADMIN / MEMBER role
> Returns ChannelUser List

```json
Request: GET /allocator/43c1ffa7-eda8-4716-b7df-6a5e0c947e7a/users
```

```json
Result: 200 OK
[
    {
        "user": {
            "id": "123e4567-e89b-12d3-a456-556642440000",
            "login": "user123",
            "display_name": "TheUser123"
        },
        "role": "MEMBER"
    },
    {
        "user": {
            "id": "92cc6909-bdf4-4a90-8f0c-8b07fa9d3282",
            "login": "megaman",
            "display_name": "megaman"
        },
        "role": "OWNER"
    },
    {
        "user": {
            "id": "4b36869e-d44a-4359-8c2c-56dd3b30664c",
            "login": "anonymous",
            "display_name": "anonymous"
        },
        "role": "MEMBER"
    }
]
```

```java
ERROR CODES
- NOT_ENOUGH_PRIVILEGES
- ELEMENT_NOT_FOUND
```

---

#### Join PUBLIC channel

**POST** /allocator/join/*{channel_id}*

> Target channel must be public

Example query :

```json
Request: POST /allocator/join/123e4567-e89b-12d3-a456-556642440000
```

```json
Result: 200 OK
```

```java
ERROR CODES
- ALREADY_JOINED
- ELEMENT_NOT_FOUND
- WRONG_CHANNEL_TYPE
```

---

#### Leave GROUP/PUBLIC channel

**POST** /allocator/leave/*{channel_id}*

> Source must have ADMIN / MEMBER role.
> 
> OWNER cannot leave channel. Transfer ownership first by editing channel
> 
> PRIVATE channel can't be leaved

Example query :

```json
Request: POST /allocator/leave/123e4567-e89b-12d3-a456-556642440000
```

```json
Result: 200 OK
```

```java
ERROR CODES
- NOT_A_MEMBER
- ELEMENT_NOT_FOUND
- WRONG_CHANNEL_TYPE
- PRIVILEGE_CONFLICT
```

## Archive

> /!\ No limitations are implemented, resulting body may be very large and take some time to complete, use with precautions
> 
> If source user has no rights to see a channel, it will be skipped in list

#### Get history since date for given list of channels

**POST** /archive/load

> Source must have OWNER / ADMIN / MEMBER role in channel.
>  Requires body with channel ID list and starting date
>  Returns one Message List per channel id, sorted by date [old, ..., new]

```json
Request: POST /archive/me
{
    "channels": [
        "7d54138f-b49c-456a-9788-4304f7ed53a0",
        "38cbb1da-8eae-4122-b0d4-fe2991b8daac"
    ],
    "since": 1551788516000
}
```

```json
Result: 200 OK
{
    "38cbb1da-8eae-4122-b0d4-fe2991b8daac": [
        {
            "id": "aa5af4db-1688-45a9-8d2b-7a19d6849772",
            "content": "I have no idea what channel this is",
            "author": {
                "id": "92cc6909-bdf4-4a90-8f0c-8b07fa9d3282",
                "login": "megaman",
                "display_name": "megaman"
            },
            "channel_id": "38cbb1da-8eae-4122-b0d4-fe2991b8daac",
            "creation_time": 1552221566359,
            "attachment": null
        },
        {
            "id": "11c51e01-6b79-4f37-b23f-bda409e1c567",
            "content": "But I'll anyways post somme messages in to it looool hahahaha",
            "author": {
                "id": "92cc6909-bdf4-4a90-8f0c-8b07fa9d3282",
                "login": "megaman",
                "display_name": "megaman"
            },
            "channel_id": "38cbb1da-8eae-4122-b0d4-fe2991b8daac",
            "creation_time": 1552221582285,
            "attachment": null
        }
    ],
    "7d54138f-b49c-456a-9788-4304f7ed53a0": [
        {
            "id": "fb7d288a-0fe2-4035-ba16-cabc7ec26b2a",
            "content": "Hello world!",
            "author": {
                "id": "92cc6909-bdf4-4a90-8f0c-8b07fa9d3282",
                "login": "megaman",
                "display_name": "megaman"
            },
            "channel_id": "7d54138f-b49c-456a-9788-4304f7ed53a0",
            "creation_time": 1552221238636,
            "attachment": null
        },
        {
            "id": "07ca382b-096a-4af1-ba13-7f4869dca3d9",
            "content": "My name is Jeff",
            "author": {
                "id": "92cc6909-bdf4-4a90-8f0c-8b07fa9d3282",
                "login": "megaman",
                "display_name": "megaman"
            },
            "channel_id": "7d54138f-b49c-456a-9788-4304f7ed53a0",
            "creation_time": 1552221247504,
            "attachment": null
        },
        {
            "id": "261f9fad-240d-4123-8e45-7590ddd73888",
            "content": "I like trains",
            "author": {
                "id": "92cc6909-bdf4-4a90-8f0c-8b07fa9d3282",
                "login": "megaman",
                "display_name": "megaman"
            },
            "channel_id": "7d54138f-b49c-456a-9788-4304f7ed53a0",
            "creation_time": 1552221252739,
            "attachment": null
        }
    ]
}
```

```java
ERROR CODES
( no error codes expected )
```

---

#### Get one channel history

**GET** /archive/*{channel_id}*/search?since=*{timestamp}*

> Source must have OWNER / ADMIN / MEMBER role in channel.
>  Returns Message List sorted by date [old, ..., new]

```json
Request: GET /archive/7d54138f-b49c-456a-9788-4304f7ed53a0/search?since=1552221231000
```

```json
Result: 200 OK
[
    {
        "id": "fb7d288a-0fe2-4035-ba16-cabc7ec26b2a",
        "content": "Hello world!",
        "author": {
            "id": "92cc6909-bdf4-4a90-8f0c-8b07fa9d3282",
            "login": "megaman",
            "display_name": "megaman"
        },
        "channel_id": "7d54138f-b49c-456a-9788-4304f7ed53a0",
        "creation_time": 1552221238636,
        "attachment": null
    },
    {
        "id": "07ca382b-096a-4af1-ba13-7f4869dca3d9",
        "content": "My name is Jeff",
        "author": {
            "id": "92cc6909-bdf4-4a90-8f0c-8b07fa9d3282",
            "login": "megaman",
            "display_name": "megaman"
        },
        "channel_id": "7d54138f-b49c-456a-9788-4304f7ed53a0",
        "creation_time": 1552221247504,
        "attachment": null
    },
    {
        "id": "261f9fad-240d-4123-8e45-7590ddd73888",
        "content": "I like trains",
        "author": {
            "id": "92cc6909-bdf4-4a90-8f0c-8b07fa9d3282",
            "login": "megaman",
            "display_name": "megaman"
        },
        "channel_id": "7d54138f-b49c-456a-9788-4304f7ed53a0",
        "creation_time": 1552221252739,
        "attachment": null
    }
]
```

```java
ERROR CODES
( no error codes expected )
```

---

#### Get channel history in xml format

**GET** /archive/export/*{channel_id}*

> Requires OWNER / ADMIN role

```jsx
Request: GET /archive/export/7d54138f-b49c-456a-9788-4304f7ed53a0
```

```json
Response: 200 OK 
<?xml version="1.0"?>
<archive>
  <message>
      <user>user</user>
      <content>Hello everyone !</content>
      <time>1234567890</time>
  </message>
  <message>
     <user>megaman</user>
     <content>Hi!</content>
     <time>1234575120</time>
 </message>
</archive>
```

```java
ERROR CODES
- NOT_ENOUGH_PRIVILEGES
- ELEMENT_NOT_FOUND
```

## Entitites

#### Default response

> Returned when a response body is expected, but server was unable to provide it
> 
> REST code returned will be always 400 Bad Request
> 
> **code** parameter will be alwyas 0 for CutomErrors ( ERROR CODES )

```java
    "code": Integer,
    "message": String,
    "exception": EnumCustomErrorCode
}
```

---

#### Login

```json
{ 
    "login": String,
    "password": String,
    "display_name": String
}
```

---

#### LoginResult

```json
{ 
    "code": EnumCustomErrorCode,    
    "success": boolean,
    "attempt": Integer,
    "locked": boolean,
    "token": String,
    "message": String,
    "id": UUID
}
```

---

#### Message

```json
{ 
    "content": String,
    "author": User,
    "channel_id": UUID,
    "creation_time": Timestamp
}
```

---

#### ChannelUser

```json
{ 
    "user": User,
    "role": EnumChannelRole
}
```

---

#### User

```json
{ 
    "id": UUID,
    "login": String,
    "display_name": String
}
```

---

#### Channel

```json
{ 
    "id": UUID,
    "name": String,
    "type": EnumChannelType,
    "members": [
        ChannelUser,
        ...
    ],
    "owner": User,
    "join_time": Timestamp
}
```

---

#### Complex archive query

```json
{
 "channels": [
 UUID,
 UUID,
 ...
 ],
 "since": Timestamp
}
```

---

#### EnumChannelType

```java
PUBLIC
GROUP
PRIVATE
```

**PUBLIC**: free join and leave, visible to everyone   
**GROUP**: users only added by OWNER or MEMBER   
**PRIVATE**: 2 users only, both are MEMBER, no join/leave possible   

---

#### EnumChannelRole

```java
OWNER
ADMIN
MEMBER
NONE
```

**OWNER**: can add/remove users, manage channel, add/remove roles, cannot be removed by ADMIN, can give ownershep to someone else, can export history   
**ADMIN**: can add/remove users, manage channel, and export history   
**MEMBER**: can read, write, join public and leave   
**NONE**: no permission    

---

#### EnumCustomErrorCode

> Details about specific custom error codes can be found in endpoints details
> 
> Exceptions listed here are generic custom error codes

```java
REST_EXCEPTION
BODY_CHECK_FAIL
MISSING_FIELD
SERVER_ERROR
```

## Push server

This is an additionnal feature used to provide notifications in real time.

### Entites compatible with push service

> Push service is based on in/out streams
> 
> Payload shoud be json only
> 
> There is one type of payload allowed from the client and two from the server

#### Handshake

```java
CONNECTION REQUEST client >> server
{
    "token": "tokentokentokentokentokentokentokentokentokentokentokentokentoke"
}
```

```java
SERVER GREETING client << server
{  
    "code":0,
    "message":"Connected to push notification service",
    "exception":"SUCCESS"
}
```

#### Notification

> **type** is event type, ex NEW_MESSAGE or USER_JOIN, read more in **event types** section
> 
> **channel_id** is the channel concerned by the event, for example by a USER_JOIN,
> for USER_ONLINE and others event not related to a particular channel, this will be the default channel id
> 
> **body** contains the same data that would be provided by the api to allow quick local data update

```json
SERVER EVENT client << server
{  
    "type":"USER_ONLINE",
    "channel_id":"f2f62cc7-da3c-49b0-b7aa-79f7001c6afc",
    "body":{  
        "id":"123e4567-e89b-12d3-a456-556642440000",
        "login":"user123",
        "display_name":"TheUser123",
        "online": true
    }
}
```

### Connecting to push service

> To connect to push service you need to have a valid authorisation token ( provided on login )
> 
> You can create one push connection per token
> 
> Push service is listening to messages from client only once, during the hand shake. No futher data should be sent to push service, as it would be ignored
> 
> Push service can be found on port 8001 by default

1. Open the socket

2. Send handskae

3. Wait for code 0 and exception SUCCES, otherwise refer to message for connection refuse reason

4. You will now receive real time notifications

### Listening to events

> You schould be constantly listening to your input stream for a new line containing a server event
> 
> No client acknowledgement is requied nor supported

### Event types

> Event types lets you know what event just happend and what type of content will be found in the event body

```markdown
NEW_MESSAGE     : new message have been posted
    body: Message
NEW_CHANNEL     : new channel has been created and you are a member of it (or PUBLIC channel)
    body: Channel
CHANNEL_UPDATED : a channel that you are member have been updated
    body: Channel
USER_JOIN       : a user joined a channel of which you are member
    body: User
USER_LEAVE      : a user leave a channel of which you are member
    body: User
USER_REGISTER   : a new user registed
    body: User
USER_ONLINE     : a user has connected to push service
    body: User
USER_OFFLINE    : a user has disconnected from the push service
    body: User
```

# Changelog

Semantic Versioning 2.0.0

## LATEST

- 1.0.1 - 15/03/2019 - XML export

  Added get xml chanel archive 

  

## ALL

- 1.0.0 - 14/03/2019 - First public release

  Added run instructions

- 0.0.7 - 13/03/2019 - push service, online indicator, channel joined indicator

  Added push service doc, online indicator in /users/search, online parameter to /users/search, channel joined indicator to GET /channels

- 0.0.6 - 10/03/2019 - snake_case and corrections

  Method corrections, changed field naming policy to snake_case, added missing examples

- 0.0.5 - 10/03/2019 - CustomErrors, HealthCheck and Examples

  Modified DefaultApiResponse to add custom error codes. Channel endpoint adjustements, HealthCheck added to doc, added more query/response examples, added possibility to change display name

- 0.0.4 - 09/03/2019 - Authorization service adjustemnts

  Added required modifications to login, logout and register enpoints

  0.0.3 - 05/03/2019 - Endpoints split by body entities

  Splitted endpoints by entity contained in body to solve technichal back-end constraint 

- 0.0.2 - 10/02/2019 - Auth/User endpoints rearrangement

  Separated auth endpoint from user endpoint, changed login http method, changes paths to more intuitive

- 0.0.1 - 24/01/2019 - Initial project

    Added first api specifications

- 0.0.0 - 24/01/2019 - File init

    Added this file, lol

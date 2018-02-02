#!/usr/bin/python
import os, socket, math, urllib.request, json, boto3, sys, datetime, coin_spot_price

dynamodb_client = boto3.client('dynamodb')
btcAddress = os.environ['BTC_ADDRESS']
def lambda_handler(event, context):
    target_url = "https://api.nicehash.com/api?method=stats.provider.ex&addr=%s" % (btcAddress)
    try:
        with urllib.request.urlopen(target_url) as url:
            if not url:
                return
            data = json.loads(url.read().decode())
            payments = data['result']['payments']
            
            for payment in payments:
                #print(json.dumps(payment))
                last_payment = dynamodb_client.get_item(TableName='payments',  Key={'pool':{'S':"nicehash"}, 'time': {'N':str(payment['time'])}})
                if not 'Item' in last_payment:
                    btc_spot_price = coin_spot_price.getCoinSpotPrice(str(payment['time']), 'BTC')
                    #print(str(payment['time']))
                    item = {
                        'pool': {'S': "nicehash"},
                        'time': {'N': str(payment['time'])},
                        'amount': {'N': str(payment['amount'])},
                        'fee': {'N': str(payment['fee'])},
                        'usd': {'N': str(btc_spot_price)},
                    }
                    dynamodb_client.put_item(TableName="payments", Item=item)
    except:
        print("Unexpected error:", sys.exc_info())

    return
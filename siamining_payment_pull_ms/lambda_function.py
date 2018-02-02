#!/usr/bin/python
import os, socket, math, urllib.request, json, boto3, sys, datetime, coin_spot_price

dynamodb_client = boto3.client('dynamodb')
siaAddress = os.environ['SIA_ADDRESS']
def lambda_handler(event, context):
    target_url = "https://siamining.com/api/v1/addresses/%s/payouts" % (siaAddress)
    try:
        with urllib.request.urlopen(target_url) as url:
            if not url:
                return
            payments = json.loads(url.read().decode())
            
            for payment in payments:
                #print(json.dumps(payment))
                last_payment = dynamodb_client.get_item(TableName='payments',  Key={'pool':{'S':"siamining"}, 'time': {'N':str(payment['time'])}})
                #print(last_payment)
                if not 'Item' in last_payment:
                    sia_spot_price = coin_spot_price.getCoinSpotPrice(str(payment['time']), 'SC')
                    item = {
                        'pool': {'S': "siamining"},
                        'time': {'N': str(payment['time'])},
                        'amount': {'N': str(payment['amount'])},
                        'usd': {'N': str(sia_spot_price)},
                    }
                    dynamodb_client.put_item(TableName="payments", Item=item)
    except:
        print("Unexpected error:", sys.exc_info())

    return
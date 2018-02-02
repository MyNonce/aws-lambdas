#!/usr/bin/python
import os, socket, math, urllib.request, json, boto3, sys, datetime

dynamodb_client = boto3.client('dynamodb')
ethosId = os.environ['ETHOS_ID'].split()
def lambda_handler(event, context):
    for dashboard in ethosId:
        #print(dashboard)
        target_url = "http://%s.ethosdistro.com/?json=yes" % (dashboard)
        try:
            with urllib.request.urlopen(target_url) as url:
                if not url:
                    continue
                data = json.loads(url.read().decode())
                rigs = data['rigs']
                for key in rigs.keys():
                    rigData = rigs[key]
                    t = datetime.datetime.now()
                    #print(json.dumps(rigData))
                    item = {
                        'name': {'S': key},
                        'datetime': {'S': t.strftime('%m/%d/%Y %H:%M:%S')},
                        'condition': {'S': rigData['condition']},
                        'version': {'S': rigData['version']},
                        'miner': {'S': rigData['miner']},
                        'gpus': {'S': rigData['gpus']},
                        'hash': {'S': str(rigData['hash'])},
                        'temp': {'S': rigData['temp']},
                        'ip': {'S': rigData['ip']},
                        'uptime': {'S': rigData['uptime']}
                    }
                    dynamodb_client.put_item(TableName="rigData", Item=item)
        except:
            print("Unexpected error:", sys.exc_info()[0])

    return
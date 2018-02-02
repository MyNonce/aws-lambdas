from enum import Enum
import json, boto3

# Create CloudWatch client
cloudwatch = boto3.client('cloudwatch')

print('Checking miner conditions')

class Condition(Enum):
    MINING = 0
    THROTTLE = 1
    STUCK_MINERS = 2
    UNREACHABLE = 3
    
conditions = {
    'mining': Condition.MINING, 
    'throttle': Condition.THROTTLE,
    'stuck_miners': Condition.STUCK_MINERS,
    'unreachable': Condition.UNREACHABLE
}

def lambda_handler(event, context):
    for record in event['Records']:
        dbrecord = record['dynamodb']
        if 'NewImage' not in dbrecord:
            continue
        
        condition = dbrecord['NewImage']['condition']['S']
        rigName = dbrecord['NewImage']['name']['S']
        # Put custom metrics
        cloudwatch.put_metric_data(
            Namespace='MYNONCE/MINERS',
            MetricData=[
                {
                    'MetricName': 'CONDITION',
                    'Dimensions': [
                        {
                            'Name': 'Rig',
                            'Value': rigName
                        },
                    ],
                    'Unit': 'Count',
                    'Value': conditions[condition].value
                },
            ]
        )
    print('Successfully processed {} records.'.format(len(event['Records'])))       
    return 'Successfully processed {} records.'.format(len(event['Records']))

import json, boto3

# Create CloudWatch client
cloudwatch = boto3.client('cloudwatch')

print('Checking miner conditions')

def lambda_handler(event, context):
    minercnt = 0
    for record in event['Records']:
        dbrecord = record['dynamodb']
        if 'NewImage' not in dbrecord:
            continue
        
        condition = dbrecord['NewImage']['condition']['S']
        if condition != 'mining':
            minercnt = minercnt + 1
            #print("ERROR: " + dbrecord['Keys']['name']['S'] + ": " + dbrecord['NewImage']['ip']['S'] + ": " + dbrecord['NewImage']['condition']['S'])

    # Put custom metrics
    cloudwatch.put_metric_data(
        Namespace='MYNONCE/MINERS',
        MetricData=[
            {
                'MetricName': 'UNHEALTH_MINERS',
                'Dimensions': [
                    {
                        'Name': 'Healthy miners',
                        'Value': 'Rigs'
                    },
                ],
                'Unit': 'Count',
                'Value': minercnt
            },
        ]
    )
    #print('Successfully processed {} records.'.format(len(event['Records'])))       
    return 'Successfully processed {} records.'.format(len(event['Records']))

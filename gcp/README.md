# GCP Image Build

## Prerequisites

- the account needs to have a Service Account that allows to build images outside of GCP
  - it must have the following roles:
    - `Compute Instance Admin (v1)` 
    - `Compute Viewer`
    - `Service Account User`
- the Service Account needs to have a key (for authentication)
- the `GOOGLE_APPLICATION_CREDENTIALS` environment variable needs to be set and point to the key file, e.g. `GOOGLE_APPLICATION_CREDENTIALS=/path/to/ACCOUNT_NAME-465523cf6ab.json` 

## Building the image

Start by checking which images have been published in order to know which version to use for the new image:

```shell
gcloud compute images list --filter="name~'aeron-premium-.*'"
```

Build the image using [Packer](https://www.packer.io/):

```shell
packer build \
-var 'project_id=PROJECT_ID' \
-var 'image_name=aeron-premium-subscription-N' \
-var 'service_account_email=image-builder@ACCOUNT.iam.gserviceaccount.com' \
aeron.pkr.hcl
```

Finally, make the image public:

```shell
gcloud compute images add-iam-policy-binding aeron-premium-subscription-N --member=allAuthenticatedUsers --role=roles/compute.imageUser
```

Finally, head over to the [Producer Portal](https://console.cloud.google.com/producer-portal) and update the deployment package configuration to point to the new image. Make sure to update it both in the `Deployment Image Source` section as well as the `Deployment Manager Configuration` section (by pressing the `Edit` button).